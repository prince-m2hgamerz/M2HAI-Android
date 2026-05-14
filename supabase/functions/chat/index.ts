import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

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
      const errorData = await response.json()
      console.error("NVIDIA API Error:", errorData)
      return new Response(JSON.stringify({
        error: "NVIDIA Chat API failed",
        upstream_status: response.status,
        details: errorData
      }), {
        status: response.status,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    if (stream) {
      return new Response(response.body, {
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
