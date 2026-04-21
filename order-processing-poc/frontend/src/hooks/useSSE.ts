import { useEffect, useRef, useState } from 'react'

interface UseSSEOptions { onMessage: (data: unknown) => void; onError?: (e: Event) => void }

export function useSSE(url: string | null, options: UseSSEOptions) {
  const [connected, setConnected] = useState(false)
  const optsRef = useRef(options); optsRef.current = options

  useEffect(() => {
    if (!url) return
    const es = new EventSource(url)
    es.onopen = () => setConnected(true)
    es.onmessage = (e) => { try { optsRef.current.onMessage(JSON.parse(e.data as string)) } catch { optsRef.current.onMessage(e.data) } }
    es.onerror = (e) => { setConnected(false); optsRef.current.onError?.(e) }
    return () => { es.close(); setConnected(false) }
  }, [url])

  return { connected }
}
