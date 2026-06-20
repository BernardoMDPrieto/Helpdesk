# 🎫 Helpdesk SLA Manager

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Angular](https://img.shields.io/badge/Angular-21-red)
![Docker](https://img.shields.io/badge/Docker-ready-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

Sistema de gestão de tickets de suporte com controle automático de SLA, priorização, notificações em tempo real e dashboard de métricas. Desenvolvido para simular um cenário real de controle de chamados, com regras de negócio inspiradas na experiência prática do autor na área.

---

## 📌 Sobre o projeto

Plataformas de helpdesk reais precisam lidar com prazos de atendimento (SLA), priorização de chamados e visibilidade para gestores. Este projeto implementa esse fluxo completo: desde a abertura do ticket pelo cliente até o monitoramento automático de prazos e a geração de relatórios de performance.

O objetivo é ir além de um CRUD básico — o sistema inclui autenticação com controle de papéis, processamento em segundo plano (scheduler de SLA), comunicação em tempo real via WebSocket e auditoria completa de alterações nos tickets.

---

## 🚧 Status do projeto

Em desenvolvimento ativo — última atualização: **junho/2026**

- [x] Estrutura do projeto, Docker Compose e CI configurados
- [x] Autenticação e controle de acesso (JWT + Spring Security)
- [x] Cadastro de usuários com envio de e-mail para primeiro acesso
- [ ] CRUD de tickets com filtros e paginação
- [ ] SLA scheduler e alertas em tempo real
- [ ] Dashboard de métricas e relatórios
- [ ] Testes de integração adicionais
- [ ] Deploy em produção

> 💡 As seções de **Demo** e **Como rodar localmente** serão atualizadas conforme cada fase for concluída.

---

## 🛠️ Tecnologias

**Backend**
- Java 21
- Spring Boot (Web, Security, Data JPA, WebSocket, Validation)
- PostgreSQL
- Redis (cache)

**Frontend**
- Angular
- TypeScript

**Infraestrutura**
- Docker / Docker Compose
- GitHub Actions (CI/CD)

---

## 🏗️ Arquitetura

<img width="2800" height="1680" alt="helpdesk_architecture" src="https://github.com/user-attachments/assets/1da5ddb4-e58e-4261-b039-9f4c455fc7bc" />


### Decisões técnicas

- **Por que SLA calculado e verificado via job agendado?** Simula um cenário real onde o sistema precisa reagir a prazos sem depender de uma requisição do usuário.
- **Por que WebSocket em vez de polling?** Reduz carga no backend e fornece atualização instantânea para supervisores monitorando violações de SLA.
- **Por que DTOs separados das entidades?** Evita expor a estrutura interna do banco na API e facilita versionamento do contrato com o frontend.

---

## ⚙️ Funcionalidades

| Funcionalidade | Status |
|---|---|
| Login / Registro com JWT | ✅ Concluído |
| Controle de acesso por papel (Cliente, Agente, Supervisor, Admin) | ✅ Concluído |
| Cadastro de senha para novos usuários via autenticação por e-mail | ✅ Concluído |
| Abertura e gestão de tickets | 🚧 Em progresso |
| Filtros e paginação de tickets | ⏳ Planejado |
| Histórico de alterações (audit log) | ⏳ Planejado |
| Cálculo automático de SLA | ⏳ Planejado |
| Alertas de violação de SLA em tempo real | ⏳ Planejado |
| Notificação por e-mail | ⏳ Planejado |
| Dashboard de métricas | ⏳ Planejado |
| Exportação de relatórios (PDF/CSV) | ⏳ Planejado |

---

## 🚀 Como rodar localmente

### Pré-requisitos
- Docker e Docker Compose

### Variáveis de ambiente

Antes de subir o projeto, crie um arquivo `.env` na mesma pasta do `docker-compose.yml` com as seguintes variáveis:

```
env
HELPDESK_DB_USER=seu_usuario
HELPDESK_DB_PASS=sua_senha
HELPDESK_JWT_SECRET=sua_chave_secreta_jwt
HELPDESK_MAIL_ADDRESS=seu_email@exemplo.com
HELPDESK_MAIL_PASS=sua_senha_de_app
HELPDESK_MAIL_HOST=smtp.exemplo.com
```

> 💡 Um arquivo `.env.example` está disponível no repositório como referência.

### Subindo o projeto

\```
bash
# Clone o repositório
git clone [https://github.com/seu-usuario/helpdesk-sla-manager.git](https://github.com/BernardoMDPrieto/Helpdesk.git)

#As imagens de dockerfile estão no root Client e do Server. Monte primeiro as imagens antes de subir o compose

# Suba todos os serviços (builda backend e frontend automaticamente)
docker-compose up --build
\```

A aplicação estará disponível em:
- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`


## 🗺️ Roadmap

1. **Fundação** — repositório, Docker, CI ✅
2. **Autenticação** — JWT, Spring Security, cadastro com e-mail de primeiro acesso ✅
3. **Core de tickets** — CRUD, SLA, histórico 🚧
4. **Tempo real** — WebSocket, scheduler de SLA, notificações ⏳
5. **Frontend** — Angular, guards de rota, telas de autenticação e tickets ⏳
6. **Dashboard e relatórios** — métricas e exportação ⏳
7. **Qualidade e entrega** — testes, seed de dados, deploy (CD) ⏳

## 👤 Autor

Desenvolvido por **Bernardo Prieto** como projeto de portfólio.

[LinkedIn](https://www.linkedin.com/in/bernardoprieto) · [GitHub](https://github.com/BernardoMDPrieto)

---
