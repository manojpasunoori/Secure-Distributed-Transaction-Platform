FROM ubuntu:24.04 AS builder
RUN apt-get update && apt-get install -y build-essential cmake libssl-dev
WORKDIR /app
COPY . .
RUN cmake -S . -B build && cmake --build build --config Release

FROM ubuntu:24.04
RUN apt-get update && apt-get install -y libssl3 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/build/validator /app/validator
EXPOSE 9443
CMD ["/app/validator"]
