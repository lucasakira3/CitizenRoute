import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./styles/base.css";
import "./styles/componentes.css";
import "./styles/paginas.css";
import "./styles/mapa.css";
import "./styles/rota.css";
import "./styles/movimento.css";

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
