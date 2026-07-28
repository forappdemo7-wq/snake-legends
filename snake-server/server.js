const express = require('express');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { 
  cors: { origin: "*" } 
});

const PORT = process.env.PORT || 3000;
const TICK_RATE = 30; // 30 updates per second
const rooms = new Map();

app.get('/', (req, res) => {
  res.json({
    status: 'online',
    app: 'Snake Legends Authoritative Backend Server',
    activeRooms: rooms.size,
    timestamp: Date.now()
  });
});

io.on('connection', (socket) => {
  console.log(`[Connect] Player connected: ${socket.id}`);

  socket.on('join_room', ({ roomId, playerName }) => {
    const finalRoomId = roomId || 'LOBBY-VIPER';
    socket.join(finalRoomId);
    socket.roomId = finalRoomId;

    if (!rooms.has(finalRoomId)) {
      rooms.set(finalRoomId, {
        id: finalRoomId,
        players: new Map(),
        orbs: generateInitialOrbs(30)
      });
    }

    const room = rooms.get(finalRoomId);
    const player = {
      id: socket.id,
      name: playerName || 'Player',
      x: (Math.random() - 0.5) * 2000,
      y: (Math.random() - 0.5) * 2000,
      angle: 0,
      speed: 5,
      score: 0
    };

    room.players.set(socket.id, player);
    io.to(finalRoomId).emit('player_joined', { playerId: socket.id, playerName: player.name });
    console.log(`[Join] ${player.name} (${socket.id}) joined room: ${finalRoomId}`);
  });

  socket.on('input_stream', (data) => {
    if (!socket.roomId) return;
    const room = rooms.get(socket.roomId);
    if (!room) return;

    const player = room.players.get(socket.id);
    if (player) {
      if (typeof data.angle === 'number') player.angle = data.angle;
      player.speed = data.boosting ? 9 : 5;
    }
  });

  socket.on('disconnect', () => {
    if (socket.roomId && rooms.has(socket.roomId)) {
      const room = rooms.get(socket.roomId);
      room.players.delete(socket.id);
      io.to(socket.roomId).emit('player_left', { playerId: socket.id });
      if (room.players.size === 0) rooms.delete(socket.roomId);
    }
    console.log(`[Disconnect] Player disconnected: ${socket.id}`);
  });
});

// Authoritative 30 FPS Server Game Loop
setInterval(() => {
  rooms.forEach((room) => {
    room.players.forEach((player) => {
      player.x += Math.cos(player.angle) * player.speed;
      player.y += Math.sin(player.angle) * player.speed;
    });

    const snapshot = {
      timestamp: Date.now(),
      players: Array.from(room.players.values()),
      orbs: room.orbs
    };

    io.to(room.id).emit('state_snapshot', snapshot);
  });
}, 1000 / TICK_RATE);

function generateInitialOrbs(count) {
  const orbs = [];
  for (let i = 0; i < count; i++) {
    orbs.push({
      id: `orb_${i}_${Math.random().toString(36).substring(2, 7)}`,
      x: (Math.random() - 0.5) * 2500,
      y: (Math.random() - 0.5) * 2500,
      value: 10
    });
  }
  return orbs;
}

server.listen(PORT, () => {
  console.log(`🚀 Snake Legends Server running on port ${PORT}`);
});
