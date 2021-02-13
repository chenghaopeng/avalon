# 阿瓦隆

阿瓦隆游戏的在线版本

目前理论支持无数个房间同时游戏，每个房间支持 6 - 8 人游戏

游戏规则、人物名字等不一定与你熟知的版本相同

---

后端采用 Java 11 + SpringBoot

```
cd server
mvn package
java -jar ./target/avalon-xx-xx.jar
```

前端采用 React.js 17

```
cd client
yarn
yarn start
yarn build
```

---

**注意，本系统极其不健壮，只适合与你熟悉的“不会作恶的”用户使用。**
