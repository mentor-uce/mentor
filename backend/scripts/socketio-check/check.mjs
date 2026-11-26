import { io } from "socket.io-client";
let input = "";
for await (const chunk of process.stdin) input += chunk;
const { url, token } = JSON.parse(input);
const socket = io(url, {
  path: "/socket.io", transports: ["websocket"], reconnection: false,
  auth: { token }, extraHeaders: { Origin: "http://localhost:5173" },
});
const timer = setTimeout(() => { socket.disconnect(); console.error("Socket.IO JS check timed out"); process.exit(1); }, 10000);
socket.on("connect", () => socket.emit("ping", "hello"));
socket.on("pong", value => {
  clearTimeout(timer); socket.disconnect();
  if (value !== "pong") { console.error("Unexpected pong"); process.exit(1); }
  console.log("Socket.IO JavaScript client4.8.4 authenticated ping/pong: passed");
  process.exit(0);
});
socket.on("connect_error", () => { clearTimeout(timer); socket.disconnect(); console.error("Socket.IO connection rejected"); process.exit(1); });
