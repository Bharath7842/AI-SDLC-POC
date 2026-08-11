// Default/fallback runtime config. Overwritten by docker-entrypoint.sh
// at container start using the API_URL environment variable.
window.__env = {
  apiUrl: 'http://localhost:8080',
};
