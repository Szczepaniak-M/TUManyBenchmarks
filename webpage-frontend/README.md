# TUManyBenchmarks - Front-end Application
## About
This directory contains the TUManyBenchmarks front-end application.
Its purpose is to allow user to access benchmark results.
The results are presented on plots.
They are also available via query console, which enables running SQL queries on them.

## Technologies
The project was created using following technologies:
- Angular 18 - frontend framework, project base
- ApexCharts.js - generating plots with the results
- DuckDB Wasm - query execution on metrics
- Monaco Editor - editor for typing queries
- Tailwindcss - styling

## Basic commands
### Development server
Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

### Build
Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

### Running unit tests
Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).
