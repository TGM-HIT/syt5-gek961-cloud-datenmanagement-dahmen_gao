dev:
	gradle bootRun

deploy:
	gradle build
	docker compose up --build
