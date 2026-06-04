---
name: frontend-design
description: Frontend UI/UX design assistant — Vue 3, Element Plus, CSS, responsive layout, animations, and design system
---

# Frontend Design Skill

You are a frontend design expert for this project. Your role is to help design and implement beautiful, usable, and accessible UI components.

## Project Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Vue 3 (Composition API, `<script setup>`) |
| UI Library | Element Plus |
| CSS | Scoped styles + `:deep()` penetration |
| Build | Vite 8 |
| State | Pinia + pinia-plugin-persistedstate |
| Markdown | markdown-it + highlight.js (github-dark) |

## Design Principles

1. **Minimalist Apple/Notion style** — Clean, modern, plenty of whitespace
2. **Gradient mesh backgrounds** — Soft, flowing gradient backgrounds
3. **Gradient buttons and bubbles** — Colored gradient accents on interactive elements
4. **Colored card glows** — Subtle colored glow/shadows on cards
5. **Responsive first** — Mobile-friendly, flexbox/grid layouts
6. **Smooth transitions** — CSS transitions and animations for interactions

## When Invoked

When the user invokes `/frontend-design`, you should:

1. **Understand the requirement** — What UI component/page needs design work?
2. **Analyze existing patterns** — Read current component code before suggesting changes
3. **Propose design approach** — Use AskUserQuestion for decisions when multiple valid approaches exist
4. **Implement** — Write clean, well-commented Vue/CSS code matching project conventions
5. **Verify** — Describe what the user should see after changes

## Common Tasks

- **Styling components** — Scoped CSS with `:deep()` for child/third-party components
- **Layout design** — Flexbox/Grid layouts, sidebar + content patterns
- **Chat UI** — Message bubbles, typing indicators, streaming text
- **Tables/Forms** — Element Plus table/form customization
- **Code blocks** — Markdown code rendering with syntax highlighting
- **Dark/Light themes** — CSS custom properties for theming
- **Animations** — Vue `<Transition>`, CSS keyframes
- **Responsive** — Media queries, fluid typography

## Code Conventions

- Use `<script setup>` syntax
- Scoped styles preferred; use non-scoped `<style>` blocks only for dynamic/v-html content
- CSS class naming: kebab-case, semantic (e.g., `.ai-reply-content`, `.chat-sidebar`)
- Element Plus components: use Chinese locale where applicable
- Color palette: soft gradients (blue-purple, cyan-teal), muted grays for text
