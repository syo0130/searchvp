---
layout: default
title: これは何
nav_order: 1
---

--------------------------------------------------------------------------------

## これは何ですか
一点透視図法にによる画像の消失点探索アルゴリズムを実装したアプリ


## 何に使えますか
二次元画像の消失点を探索することにより画像分析用途に流用出来ることを想定している


## 考え方
・エッジとエッジをペアで作成し、連結することにより線を生成する
・生成した線のポイント座標を求める
・デカルト積を求める
・デカルトあたりの中で最もポイントが多い点を信頼する
・それを消失点と見なす

## 使い方
・ビルドする
・青いボタンを押す
・画像を選択する
・VPが求まればダウンロードフォルダに選択した画像に白い点（VP）が加わり新たなファイルが生成される

## 問題点とか
・処理が重たいのでOOMKiller的な何かがつよい端末だと途中で処理が落ちる
→処理分散や一部のIOスレッド化で解決します、そのうち
※SDM865以上の端末でメモリが8GM以上ないと落ちます