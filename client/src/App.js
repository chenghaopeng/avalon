import { Button, Input, Checkbox, message } from 'antd';
import { UserOutlined, LockOutlined, FieldNumberOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import axios from 'axios';
import './App.less';

const getToken = () => sessionStorage.getItem("token") ?? "";
const setToken = (token) => sessionStorage.setItem("token", token);
const removeToken = () => sessionStorage.removeItem("token");

const DEBUG = true;

const server = DEBUG ? "localhost:8080" : "avalonserver.chper.cn";
const safe = DEBUG ? "" : "s";

const request = async (url, data) => {
  const result = await axios({
    method: "POST",
    url: `http${safe}://${server}/api${url}`,
    data
  });
  return result.data;
};

function App() {
  const [ws, setWs] = useState(null);
  const [connected, setConnected] = useState(false);
  const [inRoom, setInRoom] = useState(false);
  const [status, setStatus] = useState({running: false, log: "", info: "", users: [], no: "", tasking: false, actioning: false, voteUsers: [], taskUsers: []});
  
  const connect = (token) => {
    if (ws && !ws.CLOSED) ws.close();
    const nws = new WebSocket(`ws${safe}://${server}/game/${token}`);
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
  };

  const [username, setUsername] = useState("");
  const handleUsernameChange = (e) => {
    setUsername(e.target.value);
  };

  const [password, setPassword] = useState("");
  const handlePasswordChange = (e) => {
    setPassword(e.target.value);
  };

  useEffect(() => {
    if (!ws && !connected && getToken()) {
      connect(getToken());
      setUsername(sessionStorage.getItem("username") ?? "");
    }
  });

  const handleLogin = async () => {
    const result = await request("/user/login", { username, password });
    if (result.success) {
      setToken(result.data);
      connect(result.data);
      sessionStorage.setItem("username", username);
    }
    else {
      message.error("用户名或密码为空或错误！");
    }
  };

  const [roomNo, setRoomNo] = useState("");
  const handleRoomNoChange = (e) => {
    setRoomNo(e.target.value);
  };

  const handleEnterRoom = () => {
    sendMessage("enter", roomNo);
  };

  const handleLeaveRoom = () => {
    sendMessage("leave", null);
    setInRoom(false);
  };

  const handleNewRoom = () => {
    sendMessage("new", null);
  };

  const handleStartGame = () => {
    sendMessage("start", null);
  };

  const handleStopGame = () => {
    sendMessage("stop", null);
  };

  const [selected, setSelected] = useState([])
  const onTaskSelectChange = (values) => {
    setSelected(values);
  };

  const handleTaskRaise = () => {
    if (selected.length < 2) {
      message.error("请至少选择两人！");
      return;
    }
    sendMessage("task", selected);
  };

  const handleVoteTrue = () => {
    sendMessage("vote", true);
  };

  const handleVoteFalse = () => {
    sendMessage("vote", false);
  };

  const handleActionTrue = () => {
    sendMessage("action", true);
  };

  const handleActionFalse = () => {
    sendMessage("action", false);
  };

  return (
    <div className="App">
      <div className="header">阿瓦隆</div>
      { connected && <div>用户名 { username }</div> }
      { !connected && <>
        <Input placeholder="用户名" prefix={<UserOutlined />} onChange={handleUsernameChange} value={username} />
        <Input.Password placeholder="密码" prefix={<LockOutlined />} onChange={handlePasswordChange} />
        <Button type="primary" onClick={handleLogin}>注册并登录</Button>
      </> }
      { connected && !inRoom && <>
        <Input placeholder="房间号" prefix={<FieldNumberOutlined />} onChange={handleRoomNoChange} value={roomNo} />
        <Button type="primary" onClick={handleEnterRoom}>进入房间</Button>
        <Button onClick={handleNewRoom}>创建房间</Button>
      </> }
      { connected && inRoom && <>
        <div>房间号 { status.no }</div>
        { status.running && <Input.TextArea value={status.info} rows={3} /> }
        { status.running && !status.tasking && <>
          <div>选择本次任务的玩家</div>
          <Checkbox.Group options={status.users} onChange={onTaskSelectChange} />
          <Button type="primary" onClick={handleTaskRaise}>发起任务</Button>
        </> }
        { status.running && status.tasking && status.voteUsers.indexOf(username) >= 0 && <>
          <div>是否同意本次任务？</div>
          <Button type="primary" onClick={handleVoteTrue}>同意</Button>
          <Button type="primary" onClick={handleVoteFalse}>拒绝</Button>
        </> }
        { status.running && status.tasking && status.actioning && status.taskUsers.indexOf(username) >= 0 && <>
          <div>选择本次任务的行动</div>
          <Button type="primary" onClick={handleActionTrue}>红色</Button>
          <Button type="primary" onClick={handleActionFalse}>黑色</Button>
        </> }
        <Input.TextArea value={status.log} rows={10} />
        { !status.running && <Button type="primary" onClick={handleStartGame}>开始游戏</Button> }
        { status.running && <Button onClick={handleStopGame}>结束游戏</Button> }
        <Button onClick={handleLeaveRoom}>退出房间</Button>
        <div>房间内玩家：{ status.users.join(", ") }</div>
      </> }
      <a href="https://github.com/chenghaopeng/avalon">GitHub</a>
    </div>
  );
}

export default App;
