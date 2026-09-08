# Aether AI assistant setup

The AI layer is **optional and off by default**. The player works fully without it.
When enabled, Aether can use one of two providers:

| Provider | Default model | Default endpoint |
| --- | --- | --- |
| Google Gemini | `gemini-2.0-flash` | `https://generativelanguage.googleapis.com/v1beta/models/<model>:generateContent` |
| OpenRouter | `openrouter/auto` | `https://openrouter.ai/api/v1/chat/completions` |

## 1. Get an API key

- **Gemini** — create a key in Google AI Studio (`aistudio.google.com`), then paste it into Aether.
- **OpenRouter** — create a key at `openrouter.ai/keys`, then paste it into Aether.

## 2. Configure Aether

1. Open **Settings → AI assistant**.
2. Turn on **Enable AI layer**.
3. Pick **GEMINI** or **OPENROUTER**.
4. Optionally override the endpoint / model (blank = defaults above).
5. Paste the key into **API key** and tap **Save key**.
6. Tap **Test** — you should see `Success: OK`.

The key is stored in EncryptedSharedPreferences (AndroidKeyStore) and never leaves
the device except in direct HTTPS calls to the provider you selected.

## 3. Use it

Open any video → **More (⋮) → AI assistant**:

- **Explain** — what the metadata and markers say about the video.
- **Summarize** — structured summary of bookmarks / chapters / position.
- **Chapters** — proposes `MM:SS — Title` lines from your bookmarks.
- **Ask** — grounded Q&A over the current video context.
- **Command** — e.g. `jump to 12:30`, `pause`, `next`, `1.5x`. Simple commands run
  offline; anything else is sent to the model as strict JSON.
- **Translate subtitles** — translates the loaded SRT/VTT file into the target
  language and loads the result back into the player.

## Honesty rules (built into every prompt)

- The model only sees metadata, bookmarks, chapters, and text you provide.
- It is instructed never to claim it watched, saw, or heard the video.
- If context is missing, it says what is missing instead of guessing.
