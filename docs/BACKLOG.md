# FogUI Backlog

> Product roadmap and feature backlog for FogUI

---

## ✅ MVP Complete

### Core Features
- [x] Authentication (Login/Register/Logout)
- [x] API Key Management (Create, List, Revoke)
- [x] Usage Display (Request count, quota, remaining)
- [x] Account Settings (Email update, profile view)
- [x] `@fogui/react` SDK (FogUIProvider, useFogUI, FogUIRenderer)
- [x] Backend API (`/v1/chat/completions`, `/fogui/transform`)
- [x] SSE Streaming support
- [x] Docker + GitHub Actions CI/CD
- [x] NPM package publishing automation
- [x] VPS deployment (Coolify)

---

## 🟦 Shadcn/Tailwind Adapter MVP TODOs

#### Canonical Component Coverage
- [ ] Implement Table (with TableHeader, TableRow, TableCell) in shadcnAdapter
- [ ] Implement List component in shadcnAdapter
- [ ] Implement Stack and Grid layout components in shadcnAdapter
- [ ] Ensure all primitives (Button, Input, Label, Card, Badge) are present and tested

#### Prop/Variant Mapping
- [ ] Add mapping logic if shadcn components require different prop names or variant values (or document limitations for MVP)

#### Testing
- [ ] Add unit/integration tests for shadcnAdapter to verify all canonical components render correctly
- [ ] Test fallback rendering for unmapped components

#### Documentation
- [ ] Update README or add docs for shadcnAdapter usage and integration
- [ ] Provide example usage in a sample app (client or dashboard)

---

## 📋 Post-MVP Backlog

### 🔥 High Priority (Sprint 2)

#### Billing & Monetization
- [ ] **Stripe Integration**
  - Connect Stripe account
  - Create subscription plans (Free, Pro, Enterprise)
  - Webhook handling (subscription.created, invoice.paid)
  - Upgrade/downgrade flow
- [ ] **Pricing Tiers**
  - Free: 1,000 transforms/month
  - Pro: 50,000 transforms/month ($29/mo)
  - Enterprise: Unlimited (custom pricing)
- [ ] **Usage-based Billing**
  - Track transforms per user
  - Show cost estimates
  - Billing history page

#### Analytics Dashboard
- [ ] **Usage Graphs**
  - Requests over time (daily, weekly, monthly)
  - Success/error rates
  - Average response time
- [ ] **Component Usage**
  - Which component types are most used?
  - Card vs Table vs List breakdown
- [ ] **Export Data**
  - CSV export of usage logs
  - Monthly reports

---

### 📦 Medium Priority (Sprint 3-4)

#### Team Features
- [ ] **Team Accounts**
  - Invite team members
  - Role-based access (Admin, Developer, Viewer)
  - Shared API keys per team
- [ ] **Team Usage**
  - Aggregate usage across team
  - Per-member usage breakdown
  - Team billing

#### Developer Experience
- [ ] **API Documentation Viewer**
  - Interactive API docs
  - Code examples in multiple languages
  - Try API directly from dashboard
- [ ] **Webhooks**
  - Register webhook URLs
  - Events: quota_warning, transform_error, etc.
  - Webhook logs/debugging
- [ ] **Rate Limiting UI**
  - Set custom rate limits
  - Per-key rate limits
  - Burst allowance

#### Security & Monitoring
- [ ] **Audit Logs**
  - Track all API key operations
  - Login history
  - IP address tracking
- [ ] **Alerts**
  - Email when quota reaches 80%
  - Slack/Discord notifications
  - Error rate alerts
- [ ] **API Key Expiration**
  - Set expiration dates on keys
  - Auto-rotate keys
  - Key usage permissions

---

### 🌟 Nice-to-Have (Future)

#### CLI & SDK Tooling
- [ ] **`@fogui/cli`** - CLI tool for project scaffolding
  - `npx fogui init` - Initialize FogUI in existing project
  - `npx fogui add <adapter>` - Add design system adapter
  - Auto-detect TypeScript, Tailwind, existing design system
- [ ] **Adapter Templates**
  - Shadcn/Radix adapter (scaffold into user's project)
  - Material UI adapter
  - Ant Design adapter
  - Custom/Empty adapter (for enterprise design systems)
- [ ] **Chart Components** - Integrate with chart libraries (Recharts, Chart.js)

#### Advanced Analytics
- [ ] Real-time dashboard with WebSocket updates
- [ ] Latency P50/P95/P99 metrics
- [ ] Geographic usage map
- [ ] User journey visualization

#### Integration Marketplace
- [ ] Pre-built integrations (Vercel, Netlify, Supabase)
- [ ] One-click deploy examples
- [ ] Community adapters gallery
- [ ] Adapter testing/preview

#### Enterprise Features
- [ ] **SSO/SAML** integration
- [ ] **Custom domains** (api.customer.com)
- [ ] **SLA guarantees**
- [ ] **Dedicated support**
- [ ] **On-premise deployment** option
- [ ] **SOC2/HIPAA compliance** documentation

#### Community
- [ ] **Public roadmap** (Canny or similar)
- [ ] **Feature voting**
- [ ] **Community Discord**
- [ ] **Use case gallery** (showcase customer implementations)

---

## 🗺️ Roadmap Timeline

| Phase | Timeline | Focus |
|-------|----------|-------|
| **MVP** | Weeks 1-2 | Auth, API keys, basic usage |
| **Sprint 2** | Weeks 3-4 | Billing (Stripe), Analytics |
| **Sprint 3** | Weeks 5-6 | Teams, Webhooks |
| **Sprint 4** | Weeks 7-8 | Advanced security, Monitoring |
| **Future** | Q2 2026+ | Enterprise features, Integrations |

---

## 💡 Monetization Strategy

### Target Segments

| Segment | Use Case | Price Point |
|---------|----------|-------------|
| **Hobbyists** | Side projects, learning | Free tier |
| **Startups** | Production apps, <10K users | $29-99/mo |
| **Scale-ups** | High traffic, multiple apps | $299-999/mo |
| **Enterprise** | Custom SLA, on-prem | Custom contract |

### Revenue Drivers
1. **Transform volume** (primary metric)
2. **Team seats** (for collaboration)
3. **Advanced features** (webhooks, SSO, etc.)
4. **Support tiers** (community vs priority)

---

## 📊 Success Metrics

### MVP Launch Goals
- [ ] 100 signups in first month
- [ ] 10 paying customers (Pro plan)
- [ ] <500ms P95 API latency
- [ ] 99.9% uptime

### Sprint 2 Goals
- [ ] $1K MRR (Monthly Recurring Revenue)
- [ ] 5% free-to-paid conversion
- [ ] NPS score >40

---

## 🔧 Technical Debt to Address

### Testing
- [ ] Add integration tests for SSE streaming endpoint (`POST /fogui/transform/stream`) using WebTestClient
  - Test SSE event streaming (chunk, result, usage, [DONE] events)
  - Test error event handling when LLM fails
  - Test empty/null content error events
  - Note: MockMvc doesn't properly support async SSE testing with Spring Security
- [ ] Add Testcontainers-based integration test suite (PostgreSQL) for backend core flows
  - Replace H2 assumptions in key integration paths (auth, api keys, usage)
  - Run in CI as a separate profile/stage

### Infrastructure
- [ ] Set up proper monitoring (DataDog, New Relic, or Prometheus)
- [ ] Implement request queueing (Redis/BullMQ)
- [ ] Database indexing optimization
- [ ] CDN for static assets
- [ ] Multi-region deployment

### Notes
- jakarta.servlet vs org.springframework.test.web.servlet - investigate compatibility

---

**Last Updated:** February 8, 2026
