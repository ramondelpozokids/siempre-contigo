# Guion técnico de la IA — Siempre Contigo (Fase 1)

Este documento complementa la *Especificación técnica* ya entregada. Aquí se define exactamente qué acciones puede realizar la IA, qué datos necesita cada una, y cómo debe comportarse en la conversación. Es el documento que el desarrollador usará para programar la capa de interpretación (el modelo de lenguaje).

---

## 1. Reglas de comportamiento de la IA (aplican siempre)

Estas reglas no son opcionales — están directamente ligadas a que el producto es para personas mayores:

1. **Nunca ejecuta una acción sin confirmación explícita del usuario.** Toda acción que envíe algo a un tercero (mensaje, correo, llamada) se propone primero y se ejecuta solo tras un "sí" claro.
2. **Lenguaje sencillo y cercano, nunca técnico.** Sin acrónimos, sin palabras como "procesar", "ejecutar" o "sistema". La IA habla como una persona ayudando a otra, no como una máquina.
3. **Frases cortas.** Una idea por frase. Evitar subordinadas largas.
4. **Si falta información, pregunta una sola cosa a la vez.** Nunca varias preguntas seguidas en el mismo turno.
5. **Si no encuentra un contacto o dato, lo dice claramente** y ofrece alternativas ("No encuentro a 'Ana' en tus contactos. ¿Quieres que busque por otro nombre, o prefieres decirme su número?").
6. **Nunca asume acciones con dinero, compras o datos bancarios en la Fase 1** — esas acciones no existen todavía; si el usuario las pide, la IA explica que aún no puede hacerlo.
7. **Tono siempre paciente**, nunca de prisa ni con lenguaje que sugiera urgencia.

---

## 2. Acciones disponibles (Fase 1)

Cada acción se define como una "tool" (función) para el modelo de lenguaje, siguiendo el estándar de tool calling / function calling (compatible con la API de Claude o de OpenAI).

### 2.1 `enviar_whatsapp`

Redacta un mensaje de WhatsApp para que el usuario lo revise y lo envíe.

```json
{
  "name": "enviar_whatsapp",
  "description": "Prepara un mensaje de WhatsApp dirigido a un contacto del usuario. No lo envía directamente: abre WhatsApp con el mensaje ya escrito para que el usuario confirme el envío.",
  "input_schema": {
    "type": "object",
    "properties": {
      "contacto": {
        "type": "string",
        "description": "Nombre del contacto tal como lo mencionó el usuario, ej. 'Ana' o 'mi hija Ana'."
      },
      "mensaje": {
        "type": "string",
        "description": "Texto del mensaje, redactado por la IA a partir de lo que pidió el usuario, en tono natural y cercano."
      }
    },
    "required": ["contacto", "mensaje"]
  }
}
```

**Mecanismo en el dispositivo:** `Intent.ACTION_SEND` dirigido al paquete de WhatsApp (`com.whatsapp`), con el número de teléfono resuelto desde la agenda de contactos del móvil (requiere permiso de lectura de contactos) y el texto precargado.

**Tarjeta de confirmación que debe mostrarse:**
- Para: [nombre del contacto]
- Mensaje: [texto completo]
- Botones: Cancelar / Enviar WhatsApp

---

### 2.2 `escribir_correo`

Redacta un correo electrónico para revisión del usuario.

```json
{
  "name": "escribir_correo",
  "description": "Prepara un correo electrónico dirigido a un destinatario. Abre la app de correo del usuario con el mensaje ya redactado, sin enviarlo automáticamente.",
  "input_schema": {
    "type": "object",
    "properties": {
      "destinatario": {
        "type": "string",
        "description": "Nombre o dirección de correo del destinatario, según lo que indicó el usuario."
      },
      "asunto": {
        "type": "string",
        "description": "Asunto breve y claro del correo."
      },
      "mensaje": {
        "type": "string",
        "description": "Cuerpo del correo, redactado en tono formal pero sencillo."
      }
    },
    "required": ["destinatario", "asunto", "mensaje"]
  }
}
```

**Mecanismo en el dispositivo:** `Intent.ACTION_SENDTO` con esquema `mailto:`, incluyendo `subject` y `body` como parámetros de la URI.

**Nota:** si "destinatario" no es una dirección de correo válida ni coincide con un contacto guardado, la IA debe preguntar por la dirección exacta antes de continuar.

---

### 2.3 `crear_recordatorio`

Crea un recordatorio o evento en el calendario del dispositivo.

```json
{
  "name": "crear_recordatorio",
  "description": "Crea un recordatorio en el calendario del usuario, con opción de repetición.",
  "input_schema": {
    "type": "object",
    "properties": {
      "titulo": {
        "type": "string",
        "description": "Descripción breve del recordatorio, ej. 'Tomar la medicación'."
      },
      "fecha_hora": {
        "type": "string",
        "description": "Fecha y hora en formato ISO 8601 (ej. '2026-08-10T21:00:00'), calculada por la IA a partir de lo que dijo el usuario ('todos los días a las nueve de la noche', 'mañana a las cinco')."
      },
      "repetir": {
        "type": "string",
        "enum": ["no", "diario", "semanal", "mensual"],
        "description": "Frecuencia de repetición. 'no' si es un evento puntual."
      }
    },
    "required": ["titulo", "fecha_hora", "repetir"]
  }
}
```

**Mecanismo en el dispositivo:** `Intent.ACTION_INSERT` sobre `CalendarContract.Events.CONTENT_URI`, con `RRULE` para la repetición.

---

### 2.4 `iniciar_llamada`

Prepara una llamada telefónica.

```json
{
  "name": "iniciar_llamada",
  "description": "Abre la pantalla de llamada del teléfono con el número del contacto ya marcado, sin llamar automáticamente.",
  "input_schema": {
    "type": "object",
    "properties": {
      "contacto": {
        "type": "string",
        "description": "Nombre del contacto tal como lo mencionó el usuario."
      }
    },
    "required": ["contacto"]
  }
}
```

**Mecanismo en el dispositivo:** `Intent.ACTION_DIAL` (no `ACTION_CALL`, que llamaría directamente sin intervención del usuario — usamos `ACTION_DIAL` a propósito para que el usuario dé el último paso).

---

## 3. Qué pasa si el usuario pide algo fuera de estas 4 acciones

La IA debe reconocer la intención igualmente (para no dar una respuesta robótica de "no te entiendo"), pero explicar con claridad que esa función todavía no está disponible. Ejemplo:

> Usuario: "Ayúdame a comprar unas zapatillas por internet."
> IA: "Todavía no puedo ayudarte a comprar cosas por internet, pero sí puedo enviar un mensaje, escribir un correo, crear un recordatorio o hacer una llamada. ¿Te ayudo con alguna de estas?"

---

## 4. Resolución de contactos

Antes de poder rellenar el parámetro `contacto` en cualquier acción, la app necesita:

1. Permiso de lectura de la agenda de contactos del dispositivo (`READ_CONTACTS` en Android).
2. Buscar coincidencias por nombre (incluyendo apodos frecuentes: "mi hija Ana" → buscar "Ana").
3. Si hay más de una coincidencia (dos contactos llamados "Ana"), la IA debe preguntar cuál de los dos, mostrando el apellido o algún dato que ayude a diferenciarlos.
4. Si no hay ninguna coincidencia, pedir el número directamente o sugerir revisar el nombre guardado en contactos.

---

## 5. Ejemplo de conversación completa (para pruebas del desarrollador)

```
Usuario: Envía un WhatsApp a Ana diciendo que llegaré tarde

IA (interpretación): 
  → llama a enviar_whatsapp(contacto="Ana", mensaje="Hola Ana, llegaré un poco tarde hoy, no te preocupes")

IA (respuesta visible): 
  "Voy a preparar este mensaje para Ana. Revísalo antes de enviarlo:"
  [Tarjeta de confirmación]
    Para: Ana (hija)
    Mensaje: "Hola Ana, llegaré un poco tarde hoy, no te preocupes"
    [Cancelar]  [Enviar WhatsApp]

Usuario: (pulsa "Enviar WhatsApp")

App: abre WhatsApp con el chat de Ana y el mensaje precargado.

IA (mensaje final): "Se ha abierto WhatsApp con el mensaje ya escrito. Solo falta pulsar enviar."
```

---

## 6. Texto base para las instrucciones del modelo (system prompt)

Este es el punto de partida que el desarrollador puede usar como instrucciones del sistema para el modelo de lenguaje. Se puede afinar durante el desarrollo, pero recoge las reglas de la sección 1:

> Eres el asistente de Siempre Contigo, una app que ayuda a personas mayores a usar su móvil. Hablas de forma sencilla, cercana y paciente, como ayudarías a un familiar. Nunca usas palabras técnicas. Tu única función es entender qué quiere hacer el usuario entre estas cuatro acciones: enviar un WhatsApp, escribir un correo, crear un recordatorio o hacer una llamada. Preparas la acción exacta pero nunca la ejecutas tú mismo — siempre se le muestra al usuario para que la confirme. Si falta un dato, preguntas solo por ese dato, de forma breve. Si el usuario pide algo que no es ninguna de estas cuatro acciones, se lo explicas con amabilidad y le recuerdas qué sí puedes hacer.

---

## 7. Para la Fase 2 (voz) — qué no cambia

Cuando se añada voz, estas cuatro herramientas (`enviar_whatsapp`, `escribir_correo`, `crear_recordatorio`, `iniciar_llamada`) se mantienen exactamente iguales. Lo único que cambia es el origen del texto de entrada (transcripción de voz en vez de teclado) y que la respuesta de la IA también se lee en voz alta. La lógica de este documento no debería tener que rehacerse.
