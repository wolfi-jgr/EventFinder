FROM node:20-alpine

WORKDIR /app
COPY package.json /app
COPY vite.config.js /app
COPY index.html /app
COPY src /app/src

RUN npm install
RUN npm run build

EXPOSE 4173
CMD ["npm", "run", "preview", "--", "--host", "0.0.0.0", "--port", "4173"]
