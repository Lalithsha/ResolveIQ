"""Local benchmark-only OpenAI-compatible embedding endpoint (no paid API).

Install sentence-transformers in an isolated environment. Binds localhost by
default; use --host 0.0.0.0 only when a Docker client needs host access.
384-dimensional learned vectors are zero-padded to the schema's 1536 dimensions;
this preserves cosine similarity exactly and does not add semantic information.
"""
import argparse
import json
from http.server import BaseHTTPRequestHandler, HTTPServer


def main():
    from sentence_transformers import SentenceTransformer
    import torch
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=18091)
    parser.add_argument('--model', default='sentence-transformers/all-MiniLM-L6-v2')
    args = parser.parse_args()
    torch.set_num_threads(2)
    model = SentenceTransformer(args.model, device='cpu')

    class Handler(BaseHTTPRequestHandler):
        protocol_version = 'HTTP/1.1'

        def read_body(self):
            content_length = self.headers.get('Content-Length')
            if content_length is not None:
                return self.rfile.read(int(content_length))
            if self.headers.get('Transfer-Encoding', '').lower() == 'chunked':
                chunks = []
                while True:
                    size = int(self.rfile.readline().split(b';', 1)[0], 16)
                    if size == 0:
                        self.rfile.readline()
                        break
                    chunks.append(self.rfile.read(size))
                    self.rfile.read(2)
                return b''.join(chunks)
            raise ValueError('Request body length is missing')

        def do_POST(self):
            if self.path != '/v1/embeddings':
                self.send_error(404)
                return
            try:
                body = json.loads(self.read_body())
                if body['model'] != args.model:
                    raise ValueError('Model does not match loaded model')
                texts = body['input']
                if isinstance(texts, str):
                    texts = [texts]
                vectors = model.encode(texts, normalize_embeddings=True).tolist()
                dimension = int(body.get('dimensions', 1536))
                if dimension < len(vectors[0]):
                    raise ValueError('Truncation is not supported')
                payload = {'model': args.model, 'data': [
                    {'index': i, 'embedding': v + [0.0] * (dimension - len(v))}
                    for i, v in enumerate(vectors)]}
                data = json.dumps(payload).encode()
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.send_header('Content-Length', str(len(data)))
                self.send_header('Connection', 'close')
                self.end_headers()
                self.wfile.write(data)
            except (ValueError, KeyError) as error:
                self.send_error(400, str(error))

        def log_message(self, *args):
            pass

    print(f'Ready: {args.model} on {args.host}:{args.port}; CPU; normalized, zero-padded vectors', flush=True)
    HTTPServer((args.host, args.port), Handler).serve_forever()


if __name__ == '__main__':
    main()
