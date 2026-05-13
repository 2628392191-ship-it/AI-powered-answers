import axios from 'axios'

const API_BASE = '/ai/answer'
const STREAM_TIMEOUT = 120000 // 2 min timeout

export async function streamChat(userMessage, chatId, onChunk, abortSignal) {
  const url = `${API_BASE}/sse?userMessage=${encodeURIComponent(userMessage)}&chatId=${chatId}`

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), STREAM_TIMEOUT)

  // Link external abort signal if provided
  if (abortSignal) {
    abortSignal.addEventListener('abort', () => controller.abort())
  }

  try {
    const response = await fetch(url, { signal: controller.signal })
    if (!response.ok) throw new Error('Network response was not ok')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop()

      for (const line of lines) {
        // Support multi-line data (SSE spec)
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data && data !== '[DONE]') {
            onChunk(data)
          }
        }
      }
    }
  } finally {
    clearTimeout(timeoutId)
  }
}

export async function clearMemory(chatId) {
  const response = await axios.delete(`${API_BASE}/delmemory/${chatId}`)
  return response.data
}
