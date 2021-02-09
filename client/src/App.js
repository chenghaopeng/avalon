import { Button, Input, message } from 'antd';
import { UserOutlined, LockOutlined, FieldNumberOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import axios from 'axios';
import './App.less';

const getToken = () => sessionStorage.getItem("token") ?? "";
const setToken = (token) => sessionStorage.setItem("token", token);
const removeToken = () => sessionStorage.removeItem("token");

const server = "localhost:8080";

const request = async (url, data) => {
  const result = await axios({
    method: "POST",
    url: `http://${server}/api${url}`,
    data
  });
  return result.data;
};

function App() {
  const [ws, setWs] = useState(null);
  const [connected, setConnected] = useState(false);
  const [inRoom, setInRoom] = useState(false);
  const [status, setStatus] = useState({running: false, log: "", info: ""});
  
  const connect = (token) => {
    if (ws && !ws.CLOSED) ws.close();
    const nws = new WebSocket(`ws://${server}/game/${token}`);
    nws.onopen = () => { console.log("connecting..."); setConnected(true); };
    nws.onclose = () => { console.log("closed..."); removeToken(); setConnected(false); };
    nws.onerror = () => { console.error("ERROR"); removeToken(); setConnected(false); };
    nws.onmessage = (payload) => {
      const { type, data } = JSON.parse(payload.data);
      switch (type) {
        case "roomno":
          setRoomNo(data);
          break;
        case "status":
          setInRoom(true);
          setStatus(data);
          break;
        case "fail":
          message.error(data);
          break;
        default:
          message.info(payload);
          break;
      }
    };
    setWs(nws);
  };

  const sendMessage = (type, data) => {
    connected && ws.send(JSON.stringify({ type, data }));
  }

  useEffect(() => {
    if (!ws && !connected && getToken()) {
      connect(getToken());
    }
  });

  const [username, setUsername] = useState("");
  const handleUsernameChange = (e) => {
    setUsername(e.target.value);
  };

  const [password, setPassword] = useState("");
  const handlePasswordChange = (e) => {
    setPassword(e.target.value);
  };

  const handleLogin = async () => {
    const result = await request("/user/login", { username, password });
    if (result.success) {
      setToken(result.data);
      connect(result.data);
    }
    else {
      message.error("用户名或密码为空或错误！");
    }
  }

  const [roomNo, setRoomNo] = useState("");
  const handleRoomNoChange = (e) => {
    setRoomNo(e.target.value);
  }

  const handleEnterRoom = () => {
    sendMessage("enter", roomNo);
  }

  const handleLeaveRoom = () => {
    sendMessage("leave", null);
    setInRoom(false);
  }

  const handleNewRoom = () => {
    sendMessage("new", null);
  }

  const handleStartGame = () => {
    sendMessage("start", null);
  }

  const handleStopGame = () => {
    sendMessage("stop", null);
  }

  return (
    <div className="App">
      { !connected && <>
        <Input placeholder="用户名" prefix={<UserOutlined />} onChange={handleUsernameChange} />
        <Input.Password placeholder="密码" prefix={<LockOutlined />} onChange={handlePasswordChange} />
        <Button type="primary" onClick={handleLogin}>注册并登录</Button>
      </> }
      { connected && !inRoom && <>
        <Input placeholder="房间号" prefix={<FieldNumberOutlined />} onChange={handleRoomNoChange} value={roomNo} />
        <Button type="primary" onClick={handleEnterRoom}>进入房间</Button>
        <Button onClick={handleNewRoom}>创建房间</Button>
      </> }
      { connected && inRoom && <>
        { !status.running && <Button type="primary" onClick={handleStartGame}>开始游戏</Button> }
        { status.running && <Input.TextArea value={status.info} rows={3} /> }
        <Input.TextArea value={status.log} rows={10} />
        { status.running && <Button onClick={handleStopGame}>结束游戏</Button> }
        <Button onClick={handleLeaveRoom}>退出房间</Button>
      </> }
    </div>
  );
}

export default App;
