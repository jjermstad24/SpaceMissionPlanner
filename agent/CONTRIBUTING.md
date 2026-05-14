
# CONTRIBUTING.md

# Contribution Philosophy

This project prioritizes:
- modularity
- reproducibility
- extensibility
- deterministic execution
- notebook interoperability

---

# Coding Standards

## C++
- C++20
- modern RAII
- avoid raw ownership
- prefer immutable inputs
- use explicit typing where appropriate

## Python
- PEP8 compliance
- notebook-friendly APIs
- avoid hidden state

---

# Formatting

Recommended tools:
- clang-format
- black
- isort

---

# Testing Requirements

All new features require:
- unit tests
- deterministic validation
- serialization validation where applicable

---

# Pull Request Requirements

PRs should include:
- clear descriptions
- tests
- documentation updates
- rationale for architectural changes

---

# Architectural Rules

Do not:
- place simulation logic inside GUI code
- bypass mission graph execution systems
- introduce hidden mutable state
- hardcode frame assumptions
- hardcode units

---

# Documentation

New systems should include:
- architecture notes
- API examples
- notebook examples where appropriate

---

# Performance Philosophy

Optimize after correctness.

Focus first on:
- clean abstractions
- deterministic behavior
- extensibility

Then optimize:
- propagation
- optimization
- visualization
