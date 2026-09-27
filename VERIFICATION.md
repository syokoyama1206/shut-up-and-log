# Phase 1 検証結果

検証日: 2026-09-27。macOS / OpenJDK 22.0.2 / Android SDK 35 / Gradle 8.11.1。

| 対象 | 結果 |
|---|---|
| 共通コードのJVMコンパイル | 成功 |
| 共通ロジック / ViewModel / SQLiteテスト | 11件成功、失敗0、スキップ0 |
| Android Debug APK | assembleDebug成功、署名済みAPK生成 |
| DB schema version 1 出力 | 成功、1.dbをソースに同梱 |
| SQLDelight migration検証 | 成功（初期スキーマのみ。将来の移行は未実装） |
| Android端末上の起動・操作 | 未実施。adb devicesに接続端末・エミュレータなし |
| iOSフレームワーク / Xcodeアプリビルド | 未実施。Xcode本体がなく、Command Line Toolsのみ |
| iOS Simulator / 実機操作 | 未実施 |
| GitHub Actions | ワークフロー追加のみ。リモート実行未実施 |

## テスト内訳

- NutritionTest: 7件。基準量換算、4-9-4カロリー、直接入力時のPFC維持、任意単位、異常入力、ゼロ栄養値・全角数字、丸め前の集計、サジェスト順位・履歴復元。
- RepositoryTest: 3件。マスタ更新時の過去スナップショット維持・削除後の履歴、トランザクション失敗時のロールバック、DBを閉じて開き直した後の復元。
- LogViewModelTest: 1件。入力エラー、保存失敗時の入力保持、保存連打時の重複防止。

## 実行したGradleタスク

```sh
:composeApp:desktopTest
:composeApp:assembleDebug
:composeApp:generateCommonMainLogDatabaseSchema
:composeApp:verifyCommonMainLogDatabaseMigration
```

この環境ではGradleキャッシュとAndroidデバッグ署名鍵を作業フォルダへ配置し、Kotlinコンパイラをin-processで実行しました。最初の署名鍵書き込み制限は解消済みです。Android analytics設定の書き込みに関する警告は残っていますが、最終ビルドは終了コード0で成功しています。

ビルド成功は実機動作確認を意味しません。Android・iOSともに、最終的な入力操作・キーボード・画面回転・アクセシビリティと端末固有のSQLite動作を確認してください。

## ファイルの保存先

この成果物のビルド・テストはローカル環境で実行しました。既存の空リポジトリへソース一式をコピーする際は、そのリポジトリの`.git`を保持してください。ZIPにはローカルSDKパス、ビルドキャッシュ、署名鍵は含めていません。
