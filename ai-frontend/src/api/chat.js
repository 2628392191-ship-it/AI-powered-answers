import axios from 'axios'

const API_BASE = '/ai/answer'

export async function streamChat(userMessage, chatId, onChunk) {
  const url = `${API_BASE}/sse?UserMessage=${encodeURIComponent(userMessage)}&chatId=${chatId}`
  
  const response = await fetch(url)
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
      if (line.startsWith('data:')) {
        const data = line.slice(5).trim()
        if (data) {
          onChunk(data)
        }
      }
    }
  }
}

export async function clearMemory(chatId) {
  const response = await axios.delete(`${API_BASE}/delmemory/${chatId}`)
  return response.data
}
