FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy application files
COPY . .

# Build argument for API URL (set at build time)
ARG VITE_API_BASE
ENV VITE_API_BASE=${VITE_API_BASE}

# Build the app
RUN npm run build

# Install serve to run the production build
RUN npm install -g serve

# Expose port
EXPOSE 3000

# Serve the built app
CMD ["serve", "-s", "dist", "-l", "3000"]
