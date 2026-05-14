import { serve } from "https://deno.land/std@0.168.0/http/server.ts";


const NVIDIA_API_KEY = Deno.env.get('NVIDIA_API_KEY')
const NVIDIA_CHAT_URL = 'https://integrate.api.nvidia.com/v1/chat/completions'
const NVIDIA_MODELS_URL = 'https://integrate.api.nvidia.com/v1/models'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  if (!NVIDIA_API_KEY) {
    return new Response(JSON.stringify({
      error: "NVIDIA_API_KEY is not set in Supabase project secrets.",
      hint: "Run: supabase secrets set NVIDIA_API_KEY=your_key"
    }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  }

  try {
    // Handle GET request to list models
    if (req.method === 'GET') {
      const response = await fetch(NVIDIA_MODELS_URL, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${NVIDIA_API_KEY}`,
        },
      })

      if (!response.ok) {
        const error = await response.text()
        return new Response(JSON.stringify({ error: "NVIDIA Models API failed", details: error }), {
          status: response.status,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        })
      }

      const data = await response.json()
      return new Response(JSON.stringify(data), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const body = await req.json()
    const { messages, model, stream = true, temperature = 0.5, max_tokens = 1024 } = body

    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      return new Response(JSON.stringify({ error: "Messages array is required and cannot be empty" }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const response = await fetch(NVIDIA_CHAT_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${NVIDIA_API_KEY}`,
      },
      body: JSON.stringify({
        model: model || 'meta/llama-3.1-8b-instruct',
        messages,
        stream,
        temperature,
        top_p: 1,
        max_tokens,
      }),
    })

    if (!response.ok) {
      // Upstream may return non-JSON payloads (including stream-like text),
      // so never assume response.json() will succeed.
      const errorText = await response.text()
      console.error("NVIDIA API Error (text):", errorText)
      return new Response(JSON.stringify({
        error: "NVIDIA Chat API failed",
        upstream_status: response.status,
        details: errorText
      }), {
        status: response.status,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }


    if (stream) {
      // The upstream NVIDIA stream is not guaranteed to be SSE formatted.
      // The app expects SSE-ish `data: <json>` lines; normalize the stream to that.
      const stream = response.body
      if (!stream) {
        return new Response(JSON.stringify({ error: 'NVIDIA returned empty stream body' }), {
          status: 502,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        })
      }

      const decoder = new TextDecoder()
      const encoder = new TextEncoder()

      const normalized = new ReadableStream<Uint8Array>({
        async start(controller) {
          const reader = stream.getReader()
          let buffer = ''

          while (true) {
            const { value, done } = await reader.read()
            if (done) break

            buffer += decoder.decode(value, { stream: true })

            // Best-effort line splitting for SSE upstreams;
            // for raw chunk streams, JSON objects typically appear within lines anyway.
            const parts = buffer.split(/\r?\n/)
            buffer = parts.pop() ?? ''

            for (const part of parts) {
              const line = part.trim()
              if (!line) continue

              // If upstream already provides SSE frames, forward them.
              if (line.startsWith('data:')) {
                controller.enqueue(encoder.encode(line + '\n'))
                continue
              }

              // Otherwise, wrap the raw line/chunk as an SSE data payload.
              controller.enqueue(encoder.encode(`data: ${line}\n`))

              // If the chunk is an end marker, also terminate.
              if (line === '[DONE]') {
                controller.enqueue(encoder.encode('data: [DONE]\n'))
                controller.close()
                return
              }
            }
          }

          // Flush remaining buffer
          const tail = buffer.trim()
          if (tail) {
            if (tail.startsWith('data:')) {
              controller.enqueue(encoder.encode(tail + '\n'))
            } else {
              controller.enqueue(encoder.encode(`data: ${tail}\n`))
            }
          }

          controller.enqueue(encoder.encode('data: [DONE]\n'))
          controller.close()
        },
      })

      return new Response(normalized, {
        headers: {
          ...corsHeaders,
          'Content-Type': 'text/event-stream',
          'Cache-Control': 'no-cache',
        },
      })
    } else {
      const data = await response.json()
      return new Response(JSON.stringify(data), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

  } catch (error) {
    console.error("Function Error:", error)
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  }
})
