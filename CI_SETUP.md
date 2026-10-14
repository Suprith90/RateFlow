# GitHub Actions CI Pipeline for RateFlow

This document explains the GitHub Actions continuous integration pipeline implemented for the RateFlow project.

## Workflow Overview

The CI pipeline is defined in `.github/workflows/ci.yml` and executes on:
- Pushes to the `main` branch
- Pull requests targeting the `main` branch

## Workflow Steps

1. **Checkout repository** - Uses `actions/checkout@v4` to get the source code
2. **Set up JDK 21** - Uses `actions/setup-java@v4` with Temurin distribution
3. **Cache Maven dependencies** - Uses `actions/cache@v4` to speed up builds
4. **Validate Maven project** - Runs `mvn -B validate` to check project structure
5. **Compile source code** - Runs `mvn -B compile` to compile Java code
6. **Run tests (unit and integration)** - Runs `mvn -B test` to execute all tests
7. **Build application artifact** - Runs `mvn -B package -DskipTests` to create JAR file
8. **Build Docker image** - Builds Docker image as a validation step
9. **Upload test results** - Preserves test reports as artifacts (even if tests fail)
10. **Upload build artifact** - Preserves built JAR as an artifact

## Security Features

- Uses least-privilege permissions (`contents: read`)
- Does not store credentials in workflow files
- Only uses repository secrets when genuinely required (none needed for this basic setup)
- Uses maintained GitHub Actions versions (v4 for all actions)

## How Tests Affect Workflow Outcome

- The workflow will fail if any step fails (compile, test, build, etc.)
- Tests are NOT skipped to produce a green build - failing tests will cause the workflow to fail
- Test results are preserved as artifacts even when tests fail (using `if: always()`)

## Local Reproduction

You can reproduce the CI steps locally with these commands:

### 1. Validate project structure
```bash
mvn validate
```

### 2. Compile source code
```bash
mvn compile
```

### 3. Run tests (unit and integration)
```bash
mvn test
```

### 4. Build application artifact
```bash
mvn package -DskipTests
```

### 5. Build Docker image (validation step)
```bash
docker build -t rateflow-demo:local .
```

## Branch Protection and Workflow Checks

GitHub branch protection rules combined with workflow checks help prevent broken code from reaching the main branch:

### How it works:

1. **Pull Request Checks**: When a pull request is opened, the CI workflow runs automatically
2. **Required Status Checks**: Branch protection rules can be configured to require the CI workflow to pass before merging
3. **Prevention of Broken Code**: If tests fail, the workflow fails, preventing the PR from being merged until issues are fixed
4. **Main Branch Protection**: Similar protections can be applied to direct pushes to main branch

### Benefits:

- **Early Detection**: Issues are caught before code reaches main branch
- **Accountability**: Developers must fix breaking changes before their code can be merged
- **Code Quality**: Ensures all code on main branch passes tests and builds successfully
- **Fast Feedback**: Developers get immediate feedback on their changes

### Setting up Branch Protection (GitHub UI):

1. Go to Repository Settings → Branches → Branch protection rules
2. Add rule for `main` branch
3. Enable "Require a pull request before merging"
4. Enable "Require status checks to pass before merging"
5. Select the CI workflow check (should appear as "CI")
6. Optionally enable "Require review from code owners" and other protections

## Artifacts

The workflow preserves two types of artifacts:
- **Test results**: JUnit XML reports and test outputs from `target/surefire-reports`
- **Build artifact**: The compiled JAR file from `target/*.jar`

These artifacts are available for download from the workflow run page and are retained for 7 days.

## Troubleshooting

If the workflow fails:

1. Check the workflow run logs for detailed error information
2. Reproduce the failing step locally using the commands above
3. Fix the issue in your code
4. Push the fix to trigger a new workflow run

## Notes

- The Docker build step is used as a validation step to ensure the Dockerfile is correct
- No deployment to public environments is performed in this workflow
- The workflow uses the official Maven Docker image implicitly through the setup-java action
- Testcontainers are used for integration tests (Redis and Kafka), which should work in the GitHub Actions environment