# YoteihyoServlet: Microsoft Exchange Serverから予定を取得するServlet

[EWS Java API](https://github.com/OfficeDev/ews-java-api)を使って、
Microsoft Exchange Serverから、
指定した複数emailアドレスの予定を取得するServletです。

[Arduinoに接続したTFT LCDに行動予定表を表示](https://github.com/deton/yoteihyo#適用例-行動予定表)するために作成。

明日の予定までを取得してJSON形式で返します。

```
curl 'http://example.com:8080/YoteihyoServlet/yoteihyo?emails=deton@example.com,taro@example.com'`

{"taro@example.com":[{
  "startTime":1439514000,"endTime":1439517600,"subject":"会議",
  "location":"roomA","freeBusyStatus":"Busy"
 }],
 "deton@example.com":[]
}
```

* startTime, endTimeはUNIX時間[秒]。
* freeBusyStatus
 + "Tentative"(仮の予定)
 + "Free"(空き時間)
 + "OOF"(外出中。Out of Office)
 + "Busy"(予定あり)
 + "NoData"

## ビルド
パスワードや、ユーザID、Exchangeサーバホスト名は、
LocalProperties.javaにあるので変更してください。

ビルド時は、EWS Java APIのjarをlib/に置いてください。

gradle warでビルドしたwarファイルを、tomcat等に配備。

## 関連
* [Arduinoに接続したTFT LCDに行動予定表表示](https://github.com/deton/yoteihyo#適用例-行動予定表)
* [yoteibot: Microsoft Exchange Serverから予定を取得してIRCに通知するボット](https://github.com/deton/ExchangeAppointmentBot)
* [会議室予約状況取得Servlet(MeetingRoomServlet)](https://github.com/deton/presencelamp#会議室予約状況取得servletサーバ側meetingroomservlet)
