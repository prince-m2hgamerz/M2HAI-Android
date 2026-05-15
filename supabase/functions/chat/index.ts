import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const NVIDIA_API_KEY = Deno.env.get('NVIDIA_API_KEY')
const DEFAULT_GEMINI_API_KEY = "AIzaSyB2W1hw2MGtRmwJ9b1XERO0R1b7fyJ1VWs"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type, x-gemini-api-key, x-openai-api-key, x-groq-api-key',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    if (req.method === 'GET') {
      const response = await fetch('https://integrate.api.nvidia.com/v1/models', {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${NVIDIA_API_KEY}` },
      })
      const data = response.ok ? await response.json() : { data: [] }
      
      // Inject third-party models
      const thirdPartyModels = [
        { id: "gemini-flash-latest", object: "model", owned_by: "google" },
        { id: "gpt-4o", object: "model", owned_by: "openai" },
        { id: "gpt-4o-mini", object: "model", owned_by: "openai" },
        { id: "llama-3.1-70b-versatile", object: "model", owned_by: "groq" },
        { id: "llama3-groq-70b-8192-tool-use-preview", object: "model", owned_by: "groq" }
      ]
      
      data.data = [...(data.data || []), ...thirdPartyModels]
      
      return new Response(JSON.stringify(data), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    let body;
    try {
      body = JSON.parse(await req.text())
    } catch (e) {
      throw new Error(`Failed to parse body: ${e.message}`)
    }
    const { messages, model = 'meta/llama-3.1-70b-instruct', stream = true, temperature = 0.5, max_tokens = 1024 } = body

    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      return new Response(JSON.stringify({ error: "Messages array is empty" }), { status: 400, headers: corsHeaders })
    }

    const customGemini = req.headers.get('x-gemini-api-key')
    const customOpenai = req.headers.get('x-openai-api-key')
    const customGroq = req.headers.get('x-groq-api-key')

    const isGemini = model.startsWith('gemini')
    const isOpenAI = model.startsWith('gpt')
    const isGroq = model.includes('groq') || model === 'llama-3.1-70b-versatile'
    
    let response;

    if (isGemini) {
      const apiKey = customGemini || DEFAULT_GEMINI_API_KEY
      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:${stream ? 'streamGenerateContent?alt=sse' : 'generateContent'}`
      const contents = messages.map((m: any) => ({
        role: m.role === 'assistant' ? 'model' : 'user',
        parts: [{ text: m.content }]
      }))

      response = await fetch(geminiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-goog-api-key': apiKey },
        body: JSON.stringify({ contents })
      })
    } else if (isOpenAI) {
      if (!customOpenai) throw new Error("OpenAI API key missing. Add it in Settings > Custom Providers.")
      response = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${customOpenai}` },
        body: JSON.stringify({ model, messages, stream, temperature, max_tokens })
      })
    } else if (isGroq) {
      if (!customGroq) throw new Error("Groq API key missing. Add it in Settings > Custom Providers.")
      response = await fetch('https://api.groq.com/openai/v1/chat/completions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${customGroq}` },
        body: JSON.stringify({ model, messages, stream, temperature, max_tokens })
      })
    } else {
      if (!NVIDIA_API_KEY) throw new Error("NVIDIA_API_KEY is not set.")
      response = await fetch('https://integrate.api.nvidia.com/v1/chat/completions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${NVIDIA_API_KEY}` },
        body: JSON.stringify({ model, messages, stream, temperature, max_tokens })
      })
    }

    if (!response.ok) {
      const err = await response.text()
      return new Response(JSON.stringify({ error: "API failed", details: err }), { status: response.status, headers: corsHeaders })
    }

    if (stream) {
      const resStream = response.body
      if (!resStream) throw new Error('Empty stream')

      const decoder = new TextDecoder()
      const encoder = new TextEncoder()

      const normalized = new ReadableStream<Uint8Array>({
        async start(controller) {
          const reader = resStream.getReader()
          let buffer = ''

          while (true) {
            const { value, done } = await reader.read()
            if (done) break

            buffer += decoder.decode(value, { stream: true })
            const parts = buffer.split(/\r?\n/)
            buffer = parts.pop() ?? ''

            for (const part of parts) {
              const line = part.trim()
              if (!line) continue

              if (line.startsWith('data:')) {
                if (isGemini) {
                  const jsonStr = line.substring(5).trim()
                  if (jsonStr === '[DONE]') {
                    controller.enqueue(encoder.encode('data: [DONE]\n\n'))
                    continue
                  }
                  try {
                    const data = JSON.parse(jsonStr)
                    const text = data?.candidates?.[0]?.content?.parts?.[0]?.text || ''
                    if (text) {
                      const transformed = { choices: [{ delta: { content: text } }] }
                      controller.enqueue(encoder.encode(`data: ${JSON.stringify(transformed)}\n\n`))
                    }
                  } catch (e) {}
                } else {
                  controller.enqueue(encoder.encode(line + '\n\n'))
                }
                continue
              }
              controller.enqueue(encoder.encode(`data: ${line}\n\n`))
            }
          }

          const tail = buffer.trim()
          if (tail) {
            if (tail.startsWith('data:')) controller.enqueue(encoder.encode(tail + '\n\n'))
            else controller.enqueue(encoder.encode(`data: ${tail}\n\n`))
          }
          controller.enqueue(encoder.encode('data: [DONE]\n\n'))
          controller.close()
        },
      })

      return new Response(normalized, {
        headers: { ...corsHeaders, 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' },
      })
    } else {
      return new Response(JSON.stringify(await response.json()), { headers: corsHeaders })
    }
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500, headers: corsHeaders })
  }
})
