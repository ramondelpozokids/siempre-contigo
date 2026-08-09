/**
 * Backend mínimo: interpreta el texto del usuario con un LLM + las 4 tools
 * del guion técnico. La clave vive solo en el servidor, nunca en el APK.
 *
 * Proveedor activo: Groq (OpenAI-compatible), porque Anthropic/OpenAI/Gemini
 * de las claves disponibles están sin crédito/cuota.
 * Si existe ANTHROPIC_API_KEY con crédito, se puede forzar con LLM_PROVIDER=anthropic.
 *
 * POST /api/interpret
 * Body: { messages: [{ role: "user"|"assistant", content: string }, ...] }
 * Respuesta:
 *   { type: "message", text: string }
 *   { type: "action", text: string, action: { name, input } }
 */

const SYSTEM_PROMPT = `Eres el asistente de Siempre Contigo, una app que ayuda a personas mayores a usar su móvil. Hablas de forma sencilla, cercana y paciente, como ayudarías a un familiar. Nunca usas palabras técnicas. Tu única función es entender qué quiere hacer el usuario entre estas cuatro acciones: enviar un WhatsApp, escribir un correo, crear un recordatorio o hacer una llamada. Preparas la acción exacta pero nunca la ejecutas tú mismo — siempre se le muestra al usuario para que la confirme. Si falta un dato, preguntas solo por ese dato, de forma breve. Si el usuario pide algo que no es ninguna de estas cuatro acciones, se lo explicas con amabilidad y le recuerdas qué sí puedes hacer.

Reglas adicionales:
- Nunca asumes compras, banca ni dinero: si lo piden, explica que aún no puedes hacerlo.
- Una idea por frase. Frases cortas.
- Cuando tengas todos los datos necesarios, usa la herramienta correspondiente. No inventes números de teléfono ni correos: usa el nombre o dato que dio el usuario.
- Para fechas relativas ("mañana a las cinco", "todos los días a las nueve"), calcula fecha_hora en ISO 8601 con la zona horaria de España (Europe/Madrid) cuando sea posible.`;

/** Esquemas del guion técnico (sección 2), en formato OpenAI/Groq tools. */
const OPENAI_TOOLS = [
  {
    type: "function",
    function: {
      name: "enviar_whatsapp",
      description:
        "Prepara un mensaje de WhatsApp dirigido a un contacto del usuario. No lo envía directamente: abre WhatsApp con el mensaje ya escrito para que el usuario confirme el envío.",
      parameters: {
        type: "object",
        properties: {
          contacto: {
            type: "string",
            description:
              "Nombre del contacto tal como lo mencionó el usuario, ej. 'Ana' o 'mi hija Ana'.",
          },
          mensaje: {
            type: "string",
            description:
              "Texto del mensaje, redactado por la IA a partir de lo que pidió el usuario, en tono natural y cercano.",
          },
        },
        required: ["contacto", "mensaje"],
      },
    },
  },
  {
    type: "function",
    function: {
      name: "escribir_correo",
      description:
        "Prepara un correo electrónico dirigido a un destinatario. Abre la app de correo del usuario con el mensaje ya redactado, sin enviarlo automáticamente.",
      parameters: {
        type: "object",
        properties: {
          destinatario: {
            type: "string",
            description:
              "Nombre o dirección de correo del destinatario, según lo que indicó el usuario.",
          },
          asunto: {
            type: "string",
            description: "Asunto breve y claro del correo.",
          },
          mensaje: {
            type: "string",
            description: "Cuerpo del correo, redactado en tono formal pero sencillo.",
          },
        },
        required: ["destinatario", "asunto", "mensaje"],
      },
    },
  },
  {
    type: "function",
    function: {
      name: "crear_recordatorio",
      description:
        "Crea un recordatorio en el calendario del usuario, con opción de repetición.",
      parameters: {
        type: "object",
        properties: {
          titulo: {
            type: "string",
            description: "Descripción breve del recordatorio, ej. 'Tomar la medicación'.",
          },
          fecha_hora: {
            type: "string",
            description:
              "Fecha y hora en formato ISO 8601 (ej. '2026-08-10T21:00:00'), calculada por la IA a partir de lo que dijo el usuario.",
          },
          repetir: {
            type: "string",
            enum: ["no", "diario", "semanal", "mensual"],
            description: "Frecuencia de repetición. 'no' si es un evento puntual.",
          },
        },
        required: ["titulo", "fecha_hora", "repetir"],
      },
    },
  },
  {
    type: "function",
    function: {
      name: "iniciar_llamada",
      description:
        "Abre la pantalla de llamada del teléfono con el número del contacto ya marcado, sin llamar automáticamente.",
      parameters: {
        type: "object",
        properties: {
          contacto: {
            type: "string",
            description: "Nombre del contacto tal como lo mencionó el usuario.",
          },
        },
        required: ["contacto"],
      },
    },
  },
];

function buildSystemPrompt() {
  const today = new Date().toLocaleString("sv-SE", {
    timeZone: "Europe/Madrid",
    hour12: false,
  }).replace(" ", "T");
  return `${SYSTEM_PROMPT}

Hoy es ${today} (Europe/Madrid). Usa ese año y fecha al calcular fecha_hora.`;
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    "Content-Type": "application/json; charset=utf-8",
  };
}

function json(res, status, body) {
  res.statusCode = status;
  for (const [k, v] of Object.entries(corsHeaders())) {
    res.setHeader(k, v);
  }
  res.end(JSON.stringify(body));
}

function normalizeMessages(raw) {
  if (!Array.isArray(raw) || raw.length === 0) return null;
  const out = [];
  for (const m of raw) {
    if (!m || (m.role !== "user" && m.role !== "assistant")) continue;
    const content = typeof m.content === "string" ? m.content.trim() : "";
    if (!content) continue;
    if (out.length > 0 && out[out.length - 1].role === m.role) {
      out[out.length - 1].content += "\n" + content;
    } else {
      out.push({ role: m.role, content });
    }
  }
  if (out.length === 0) return null;
  if (out[0].role !== "user") {
    out.unshift({ role: "user", content: "Hola." });
  }
  return out.slice(-20);
}

function parseToolArguments(raw) {
  if (raw == null) return {};
  if (typeof raw === "object") return raw;
  try {
    return JSON.parse(raw);
  } catch {
    return {};
  }
}

async function callGroq(messages) {
  const apiKey = process.env.GROQ_API_KEY;
  if (!apiKey) {
    const err = new Error("Falta GROQ_API_KEY en el servidor");
    err.status = 500;
    throw err;
  }
  const model = process.env.GROQ_MODEL || "llama-3.3-70b-versatile";
  const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      temperature: 0.2,
      messages: [{ role: "system", content: buildSystemPrompt() }, ...messages],
      tools: OPENAI_TOOLS,
      tool_choice: "auto",
    }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const msg =
      data?.error?.message ||
      data?.error ||
      `Groq HTTP ${res.status}`;
    const err = new Error(typeof msg === "string" ? msg : JSON.stringify(msg));
    err.status = 502;
    throw err;
  }
  const choice = data.choices?.[0]?.message || {};
  const toolCall = choice.tool_calls?.[0];
  const text = (choice.content || "").trim();
  if (toolCall?.function?.name) {
    return {
      type: "action",
      text:
        text ||
        "Esto es lo que voy a preparar. Revísalo antes de continuar:",
      action: {
        name: toolCall.function.name,
        input: parseToolArguments(toolCall.function.arguments),
      },
    };
  }
  return {
    type: "message",
    text:
      text ||
      "¿Me lo puedes decir de otra forma? Puedo enviar un WhatsApp, escribir un correo, crear un recordatorio o hacer una llamada.",
  };
}

async function callAnthropic(messages) {
  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey) {
    const err = new Error("Falta ANTHROPIC_API_KEY en el servidor");
    err.status = 500;
    throw err;
  }
  const { default: Anthropic } = await import("@anthropic-ai/sdk");
  const model = process.env.ANTHROPIC_MODEL || "claude-sonnet-4-20250514";
  const client = new Anthropic({ apiKey });
  const tools = OPENAI_TOOLS.map((t) => ({
    name: t.function.name,
    description: t.function.description,
    input_schema: t.function.parameters,
  }));
  const response = await client.messages.create({
    model,
    max_tokens: 1024,
    system: buildSystemPrompt(),
    tools,
    messages,
  });
  const toolUse = (response.content || []).find((b) => b.type === "tool_use");
  const text = (response.content || [])
    .filter((b) => b.type === "text" && b.text)
    .map((b) => b.text.trim())
    .filter(Boolean)
    .join("\n\n");
  if (toolUse) {
    return {
      type: "action",
      text:
        text ||
        "Esto es lo que voy a preparar. Revísalo antes de continuar:",
      action: { name: toolUse.name, input: toolUse.input || {} },
    };
  }
  return {
    type: "message",
    text:
      text ||
      "¿Me lo puedes decir de otra forma? Puedo enviar un WhatsApp, escribir un correo, crear un recordatorio o hacer una llamada.",
  };
}

function resolveProvider() {
  const forced = (process.env.LLM_PROVIDER || "").toLowerCase().trim();
  if (forced === "anthropic" || forced === "groq") return forced;
  if (process.env.GROQ_API_KEY) return "groq";
  if (process.env.ANTHROPIC_API_KEY) return "anthropic";
  return null;
}

export default async function handler(req, res) {
  if (req.method === "OPTIONS") {
    return json(res, 204, {});
  }
  if (req.method !== "POST") {
    return json(res, 405, { error: "Método no permitido" });
  }

  const provider = resolveProvider();
  if (!provider) {
    return json(res, 500, {
      error: "Falta GROQ_API_KEY (o ANTHROPIC_API_KEY) en el servidor",
    });
  }

  let body = req.body;
  if (typeof body === "string") {
    try {
      body = JSON.parse(body);
    } catch {
      return json(res, 400, { error: "JSON inválido" });
    }
  }
  if (!body || typeof body !== "object") {
    return json(res, 400, { error: "Cuerpo vacío" });
  }

  const messages = normalizeMessages(body.messages);
  if (!messages) {
    return json(res, 400, { error: "messages es obligatorio" });
  }

  try {
    const result =
      provider === "anthropic"
        ? await callAnthropic(messages)
        : await callGroq(messages);
    return json(res, 200, result);
  } catch (err) {
    console.error("interpret error", provider, err?.message || err);
    const status = err?.status === 500 ? 500 : 502;
    return json(res, status, {
      error:
        status === 500
          ? err.message
          : "No he podido entenderte ahora. Prueba otra vez en un momento.",
    });
  }
}
