FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
ENV BACKEND_URL=http://backend:8080
ENV INTERVAL=4
CMD ["sh", "-c", "python simulator.py --backend $BACKEND_URL --interval $INTERVAL"]
