FROM node:22-alpine
WORKDIR /app
COPY package.json .
RUN npm install --omit=dev
COPY server.js .
RUN mkdir -p /data/tmp
ENV PORT=8081
EXPOSE 8081
CMD ["npm","start"]
