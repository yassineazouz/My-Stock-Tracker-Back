docker run --name postgres-container \
  -e POSTGRES_DB=portfolio_tracker \
  -e POSTGRES_USER=yassine \
  -e POSTGRES_PASSWORD=securepassword \
  -p 5432:5432 \
  -d postgres:17
