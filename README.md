# Shut Up and Log — Phase 1

「痩せたければ記録しろ」。Android / iOS向けのローカル食事・PFC記録アプリです。

## 実装範囲

- 日付ごとの食事記録、前日・翌日・今日・カレンダーによる日付選択
- 今回のPFCを直接入力、または任意の基準量から換算
- g / 個 / ml / 食、少数の摂取量・PFC
- 入力途中のPFC・推定カロリープレビュー
- 品目の部分一致サジェスト（前方一致優先、最近の利用順、最大6件）
- 履歴選択で量・単位・入力方式・PFCを復元
- P/F/C合計、4-9-4方式による推定カロリー
- SQLiteへの永続化、削除確認、保存失敗時の入力保持

直接入力では量を変更してもPFCを自動換算しません。換算する食品は「基準量あたり」を選びます。
履歴から選んだ食品の栄養値を変更して記録すると、その食品マスタを更新します。品目名を書き換えると別の食品として登録します。過去の食事記録は変更されません。

## 開発環境

固定バージョン: Kotlin 2.1.20 / Compose Multiplatform 1.8.2 / Material 3 / Gradle 8.11.1 / AGP 8.9.1 / SQLDelight 2.1.0。
Coroutines 1.10.2、kotlinx-datetime 0.6.2、共通Lifecycle ViewModel 2.9.0を使用。

- JDK 17以上（推奨17または21）
- Android SDK Platform 35、ビルドツール、Android Studio
- iOS: macOS、Xcode本体とiOS SDK、XcodeGen
- 初回ビルド時は依存関係のダウンロードにインターネット接続が必要

## Android

このディレクトリをAndroid Studioで開き、SDK設定を行います。コマンドラインの場合、`local.properties`を作成します（Git管理対象外）。

```properties
sdk.dir=/Users/YOUR_USER/Library/Android/sdk
```

```sh
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`。
`installDebug`には接続済み端末または起動済みエミュレータが必要です。

## iOS

XcodeGenで同梱の設定からXcodeプロジェクトを生成します。

```sh
cd iosApp
xcodegen generate
open ShutUpAndLog.xcodeproj
```

`iosApp`スキームとiOS Simulatorを選択して実行します。Xcodeのビルドフェーズで共有フレームワークを生成します。実機の場合はSigning Teamと必要に応じてBundle Identifierを設定してください。Xcodeをコマンドラインから使う場合も、Developer DirectoryをXcode本体に設定する必要があります。

共有フレームワークだけをビルドする場合:

```sh
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

iOSで確認する項目: 起動、キーボードと入力欄のスクロール、履歴選択、日付選択、画面回転、アプリ終了後のデータ復元、実機署名、日本語表示、VoiceOver。

## テスト

```sh
./gradlew :composeApp:desktopTest
./gradlew :composeApp:verifyCommonMainLogDatabaseMigration
```

`desktop`は共通コードとSQLiteのJVMテスト用ターゲットです。デスクトップ製品UIは含みません。
計算・入力検証・履歴候補・ViewModelの保存失敗と連打・食品更新後のスナップショット・トランザクションのロールバック・DB再オープンを検証します。

## 構成

```text
composeApp/
  src/commonMain/kotlin/com/shutupandlog/
    domain/        食品・日次記録・PFC計算・入力検証・Repository境界
    data/          SQLDelight実装、トランザクション、UUID発行
    presentation/  Compose UI、ViewModel、画面状態
  src/commonMain/sqldelight/  SQLとスキーマ
  src/androidMain/           Application、Activity、SQLiteドライバ
  src/iosMain/               UIViewController、SQLiteドライバ
  src/commonTest/            共通ロジックとViewModelのテスト
  src/desktopTest/           実SQLiteによる永続化テスト
  build.gradle.kts
iosApp/                      SwiftUI起動コード、XcodeGen設定
```

## 設計

- 単一KMPモジュール内で役割を分離。DIフレームワークは使用せず、Repositoryを注入。
- 食品と食事はUUIDで識別。食事側には品名・量・単位・PFCをスナップショット保存。
- 食品更新と食事追加は1つのSQLiteトランザクション。更新後のみStateFlowへ反映。
- Doubleで計算し、保存時には丸めず、表示時だけ小数点以下2桁に丸める。
- 空欄、負数、非有限数、ゼロの量、過大値を拒否。全角数字・全角小数点は正規化。
- 日付はローカル暦日をISO形式で保存。作成時刻はUTCのepoch milliseconds。
- 食事削除後も食品履歴を維持。バックエンド・分析SDK・ネットワーク権限なし。
- DB処理はUIスレッド外で直列化。画面回転時はViewModelが入力状態を保持。
- Phase 1では履歴をメモリ上に読み込み検索。大量データ時はRepository内でSQL検索・日付単位購読へ変更可能。
- 保存前の入力はプロセス終了後に復元しません。端末内DBの外部バックアップ/同期は未実装。

## マイグレーション

初期スキーマはversion 1。SQLDelightのスキーマ出力とmigration検証を有効化しています。
DB構造を変更する際は`.sq`に加えて`1.sqm`などを追加し、旧版DBからの移行をテストします。破壊的なDB再作成は行いません。

```sh
./gradlew :composeApp:generateCommonMainLogDatabaseSchema
./gradlew :composeApp:verifyCommonMainLogDatabaseMigration
```

Repository境界は将来の同期追加ポイントです。UUIDは共有できますが、クラウド同期を実装する際は競合解決、削除トゥームストーン、同期状態、認証を別途設計してください。

## 次のPhase

8筋肉カテゴリ・プリセット種目、重量/repのセット記録、前回記録の再利用。その後、自己ベスト・推定1RM・ボリューム・履歴グラフ、ローカルルールによる初心者メニュー提案を追加します。

## 検証結果

実際の環境での結果は `VERIFICATION.md` を参照してください。
