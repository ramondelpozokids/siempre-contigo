import { useState, useRef, useEffect } from "react";
import { MessageCircle, Mail, Bell, Phone, Check, X, Type, Users } from "lucide-react";

const BRAND = "#1B5FA8";
const BRAND_DARK = "#0F4278";
const BG = "#F4F7FB";
const AI_BUBBLE = "#EAF0F8";
const GREEN = "#1E8E5A";

const SCENARIOS = [
  {
    id: "whatsapp",
    label: "Enviar WhatsApp",
    Icon: MessageCircle,
    userText: "Envía un WhatsApp a Ana diciendo que llegaré tarde",
    aiText: "Voy a preparar este mensaje para Ana. Revísalo antes de enviarlo:",
    cardTitle: "WhatsApp a Ana",
    cardLines: [
      { label: "Para", value: "Ana (hija)" },
      { label: "Mensaje", value: "Hola Ana, llegaré un poco tarde hoy, no te preocupes 😊" },
    ],
    confirmLabel: "Enviar WhatsApp",
    openingApp: "WhatsApp",
    successText: "Se ha abierto WhatsApp con el mensaje ya escrito. Solo falta pulsar enviar.",
  },
  {
    id: "correo",
    label: "Escribir un correo",
    Icon: Mail,
    userText: "Escribe un correo al doctor Martínez pidiendo cita",
    aiText: "Esto es lo que voy a escribir. Dime si quieres cambiar algo:",
    cardTitle: "Correo al Dr. Martínez",
    cardLines: [
      { label: "Para", value: "consultas@drmartinez.es" },
      { label: "Asunto", value: "Solicitud de cita" },
      { label: "Mensaje", value: "Buenos días, me gustaría pedir una cita en los próximos días. Muchas gracias." },
    ],
    confirmLabel: "Abrir correo",
    openingApp: "tu app de correo",
    successText: "Se ha abierto tu correo con todo listo. Solo falta pulsar enviar.",
  },
  {
    id: "recordatorio",
    label: "Crear recordatorio",
    Icon: Bell,
    userText: "Recuérdame tomar la medicación todos los días a las nueve de la noche",
    aiText: "Voy a crear este recordatorio en tu calendario:",
    cardTitle: "Nuevo recordatorio",
    cardLines: [
      { label: "Título", value: "Tomar la medicación" },
      { label: "Hora", value: "21:00, todos los días" },
    ],
    confirmLabel: "Crear recordatorio",
    openingApp: "tu calendario",
    successText: "Recordatorio creado. Te avisaremos todos los días a las nueve de la noche.",
  },
  {
    id: "llamada",
    label: "Hacer una llamada",
    Icon: Phone,
    userText: "Llama a mi hijo Carlos",
    aiText: "Voy a iniciar esta llamada:",
    cardTitle: "Llamar a Carlos",
    cardLines: [{ label: "Contacto", value: "Carlos (hijo) · 600 123 456" }],
    confirmLabel: "Llamar ahora",
    openingApp: "el teléfono",
    successText: "Se ha abierto la llamada a Carlos.",
  },
];

function TypingDots() {
  return (
    <div style={{ display: "flex", gap: 4, padding: "10px 14px" }}>
      {[0, 1, 2].map((i) => (
        <span
          key={i}
          style={{
            width: 6,
            height: 6,
            borderRadius: 999,
            background: "#9AA6B2",
            animation: "sc-bounce 1s infinite",
            animationDelay: `${i * 0.15}s`,
          }}
        />
      ))}
    </div>
  );
}

function Bubble({ from, children, fontScale }) {
  const isUser = from === "user";
  return (
    <div style={{ display: "flex", justifyContent: isUser ? "flex-end" : "flex-start", marginBottom: 10 }}>
      <div
        style={{
          maxWidth: "80%",
          background: isUser ? BRAND : AI_BUBBLE,
          color: isUser ? "#fff" : "#1E2A38",
          padding: "10px 14px",
          borderRadius: 16,
          borderBottomRightRadius: isUser ? 4 : 16,
          borderBottomLeftRadius: isUser ? 16 : 4,
          fontSize: 15 * fontScale,
          lineHeight: 1.45,
        }}
      >
        {children}
      </div>
    </div>
  );
}

function ConfirmCard({ scenario, onCancel, onConfirm, fontScale }) {
  return (
    <div
      style={{
        background: "#fff",
        border: `1.5px solid ${BRAND}`,
        borderRadius: 16,
        padding: 14,
        marginBottom: 10,
        maxWidth: "88%",
      }}
    >
      <div style={{ fontWeight: 600, fontSize: 14 * fontScale, color: BRAND_DARK, marginBottom: 8 }}>
        {scenario.cardTitle}
      </div>
      {scenario.cardLines.map((l) => (
        <div key={l.label} style={{ marginBottom: 6 }}>
          <div style={{ fontSize: 11 * fontScale, color: "#7A8593", fontWeight: 600, letterSpacing: 0.3 }}>
            {l.label.toUpperCase()}
          </div>
          <div style={{ fontSize: 14 * fontScale, color: "#1E2A38" }}>{l.value}</div>
        </div>
      ))}
      <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
        <button
          onClick={onCancel}
          style={{
            flex: 1,
            padding: "10px 0",
            borderRadius: 10,
            border: "1.5px solid #D7DEE6",
            background: "#fff",
            color: "#4B5563",
            fontSize: 14 * fontScale,
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          Cancelar
        </button>
        <button
          onClick={onConfirm}
          style={{
            flex: 1.4,
            padding: "10px 0",
            borderRadius: 10,
            border: "none",
            background: BRAND,
            color: "#fff",
            fontSize: 14 * fontScale,
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          {scenario.confirmLabel}
        </button>
      </div>
    </div>
  );
}

export default function ChatPrototype() {
  const [messages, setMessages] = useState([
    { id: "m0", from: "ai", kind: "text", text: "Hola, soy tu asistente. ¿En qué te ayudo hoy?" },
  ]);
  const [phase, setPhase] = useState("idle"); // idle | thinking | confirm | opening
  const [activeScenario, setActiveScenario] = useState(null);
  const [largeText, setLargeText] = useState(false);
  const [familyToast, setFamilyToast] = useState(null);
  const scrollRef = useRef(null);
  const fontScale = largeText ? 1.25 : 1;

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, phase]);

  function pushMessage(msg) {
    setMessages((prev) => [...prev, { id: `m${Date.now()}-${Math.random()}`, ...msg }]);
  }

  function startScenario(scenario) {
    if (phase !== "idle") return;
    setActiveScenario(scenario);
    pushMessage({ from: "user", kind: "text", text: scenario.userText });
    setPhase("thinking");
    setTimeout(() => {
      pushMessage({ from: "ai", kind: "text", text: scenario.aiText });
      setPhase("confirm");
    }, 900);
  }

  function cancelScenario() {
    setPhase("idle");
    pushMessage({ from: "ai", kind: "text", text: "Vale, no he hecho nada. ¿Te ayudo con otra cosa?" });
    setActiveScenario(null);
  }

  function confirmScenario() {
    const scenario = activeScenario;
    setPhase("opening");
    pushMessage({ from: "ai", kind: "opening", app: scenario.openingApp });
    setTimeout(() => {
      setMessages((prev) => prev.filter((m) => m.kind !== "opening"));
      pushMessage({ from: "ai", kind: "success", text: scenario.successText });
      setFamilyToast(`Aviso a tu familia: "Se ha usado ${scenario.cardTitle.toLowerCase()}"`);
      setTimeout(() => setFamilyToast(null), 3200);
      setPhase("idle");
      setActiveScenario(null);
    }, 1100);
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", padding: "24px 0", fontFamily: "system-ui, -apple-system, sans-serif" }}>
      <style>{`@keyframes sc-bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-5px)} }`}</style>

      <div style={{ marginBottom: 14, textAlign: "center" }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: BRAND_DARK, letterSpacing: 0.3 }}>
          PROTOTIPO · FASE 1 (ANDROID, TEXTO)
        </div>
        <div style={{ fontSize: 13, color: "#7A8593", marginTop: 2 }}>
          Toca una acción abajo para ver el flujo completo
        </div>
      </div>

      {/* Phone frame */}
      <div
        style={{
          width: 340,
          height: 620,
          background: "#0E1420",
          borderRadius: 36,
          padding: 10,
          boxShadow: "0 20px 50px rgba(15,23,42,0.25)",
        }}
      >
        <div
          style={{
            width: "100%",
            height: "100%",
            background: BG,
            borderRadius: 26,
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
            position: "relative",
          }}
        >
          {/* Status bar */}
          <div style={{ height: 24, display: "flex", alignItems: "center", justifyContent: "center" }}>
            <div style={{ width: 90, height: 5, background: "#0E1420", borderRadius: 999, opacity: 0.15 }} />
          </div>

          {/* Header */}
          <div
            style={{
              background: BRAND,
              color: "#fff",
              padding: "10px 16px 14px",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <div>
              <div style={{ fontWeight: 700, fontSize: 16 * fontScale }}>Siempre Contigo</div>
              <div style={{ fontSize: 11 * fontScale, opacity: 0.85 }}>Tu asistente</div>
            </div>
            <button
              onClick={() => setLargeText((v) => !v)}
              aria-label="Cambiar tamaño de texto"
              style={{
                background: "rgba(255,255,255,0.18)",
                border: "none",
                borderRadius: 10,
                width: 36,
                height: 36,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#fff",
                cursor: "pointer",
              }}
            >
              <Type size={18} />
            </button>
          </div>

          {/* Family toast */}
          {familyToast && (
            <div
              style={{
                position: "absolute",
                top: 66,
                left: 12,
                right: 12,
                background: "#1E2A38",
                color: "#fff",
                borderRadius: 10,
                padding: "8px 10px",
                fontSize: 11.5 * fontScale,
                display: "flex",
                alignItems: "center",
                gap: 6,
                zIndex: 5,
                boxShadow: "0 6px 16px rgba(0,0,0,0.2)",
              }}
            >
              <Users size={14} style={{ flexShrink: 0 }} />
              {familyToast}
            </div>
          )}

          {/* Chat area */}
          <div ref={scrollRef} style={{ flex: 1, overflowY: "auto", padding: "14px 12px 6px" }}>
            {messages.map((m) => {
              if (m.kind === "opening") {
                return (
                  <div key={m.id} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 4px", color: "#7A8593", fontSize: 13 * fontScale }}>
                    <TypingDots />
                    Abriendo {m.app}…
                  </div>
                );
              }
              if (m.kind === "success") {
                return (
                  <div key={m.id} style={{ display: "flex", justifyContent: "flex-start", marginBottom: 10 }}>
                    <div
                      style={{
                        maxWidth: "82%",
                        background: "#E7F5EC",
                        color: "#155C36",
                        padding: "10px 14px",
                        borderRadius: 16,
                        borderBottomLeftRadius: 4,
                        fontSize: 14 * fontScale,
                        display: "flex",
                        gap: 8,
                        alignItems: "flex-start",
                      }}
                    >
                      <Check size={16} style={{ marginTop: 2, flexShrink: 0, color: GREEN }} />
                      <span>{m.text}</span>
                    </div>
                  </div>
                );
              }
              return (
                <Bubble key={m.id} from={m.from} fontScale={fontScale}>
                  {m.text}
                </Bubble>
              );
            })}

            {phase === "thinking" && (
              <div style={{ display: "flex", justifyContent: "flex-start", marginBottom: 10 }}>
                <div style={{ background: AI_BUBBLE, borderRadius: 16, borderBottomLeftRadius: 4 }}>
                  <TypingDots />
                </div>
              </div>
            )}

            {phase === "confirm" && activeScenario && (
              <ConfirmCard scenario={activeScenario} onCancel={cancelScenario} onConfirm={confirmScenario} fontScale={fontScale} />
            )}
          </div>

          {/* Quick actions */}
          <div style={{ padding: "8px 10px", borderTop: "1px solid #E2E8F0", background: "#fff" }}>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              {SCENARIOS.map((s) => (
                <button
                  key={s.id}
                  onClick={() => startScenario(s)}
                  disabled={phase !== "idle"}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 6,
                    padding: "10px 8px",
                    borderRadius: 12,
                    border: "1.5px solid #D7E1EC",
                    background: phase !== "idle" ? "#F1F4F8" : "#fff",
                    color: phase !== "idle" ? "#A6B0BC" : BRAND_DARK,
                    fontSize: 12.5 * fontScale,
                    fontWeight: 600,
                    cursor: phase !== "idle" ? "default" : "pointer",
                    textAlign: "left",
                  }}
                >
                  <s.Icon size={16} style={{ flexShrink: 0 }} />
                  {s.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div style={{ marginTop: 14, fontSize: 12, color: "#9AA6B2", maxWidth: 340, textAlign: "center" }}>
        El botón de tamaño de texto (arriba a la derecha) simula el modo de lectura fácil. El aviso emergente muestra lo que vería un familiar conectado — solo el hecho de que se hizo algo, nunca el contenido.
      </div>
    </div>
  );
}
