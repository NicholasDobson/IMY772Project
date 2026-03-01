# 🌱 Smart AgriCloud PWA
**IMY 772 – Web Information Systems Project (2026)**

Client: Department of Plant and Soil Sciences  
Institution: University of Pretoria  
Deployment Platform: Amazon Web Services (AWS)  

---

## 📌 Project Description

Smart AgriCloud is a cloud-based **Progressive Web Application (PWA)** designed to support agricultural research through **real-time environmental data processing**, **image-based plant health analysis**, and **secure, scalable cloud infrastructure**.

The system enables researchers and students to monitor soil and crop conditions, upload and analyse plant images using machine learning techniques, and access historical research data from any device.

---

## 🎯 Problem Statement

Current agricultural research processes rely on fragmented tools for data collection, manual analysis of plant images, and limited real-time insight. These limitations reduce research efficiency and data accessibility.

This project aims to deliver a **centralised, cloud-native solution** that integrates:
- Real-time data processing  
- Image analysis  
- Secure identity management  
- Scalable cloud infrastructure  

---

# Phase 1: Marketing Research & Prototyping

## Market Research
Target users include:
- Plant and Soil Science researchers
- Postgraduate students
- Field technicians
- Academic supervisors

Key user needs:
- Mobile and web accessibility
- Real-time data visibility
- Image-based plant analysis
- Secure role-based access
- Minimal maintenance overhead

## Stakeholder Insights
- Web-based systems preferred over native apps
- High demand for visual plant health indicators
- Need for historical and comparative data
- Strong emphasis on data security

## Competitor Analysis
| Solution Type | Strengths | Weaknesses |
|--------------|----------|------------|
| Traditional farm systems | Feature rich | Not research focused |
| Spreadsheets | Simple | No real-time support |
| IoT dashboards | Live data | Complex and unfriendly UX |

## Feature Shortlist
- User authentication and roles
- Real-time sensor dashboards
- Image upload and analysis
- Historical data analytics
- Offline-capable PWA

## Prototyping
- Low-fidelity wireframes
- UX flow diagrams
- Dashboard and image analysis mockups

---

# Phase 2: Requirements & Design

## Functional Requirements
- Secure user authentication
- Upload and process plant images
- Display real-time sensor data
- Store and retrieve historical data
- Integrate external APIs

## Non-Functional Requirements
- Scalable serverless architecture
- High availability
- Secure cloud infrastructure
- Responsive UI
- Performance under load

## UX Flow
1. User authentication  
2. Dashboard access  
3. Real-time data view  
4. Image upload  
5. Analysis results  
6. Historical data review  

## System Architecture
- **Frontend:** React-based PWA
- **Backend:** AWS Lambda + API Gateway
- **Storage:** S3, DynamoDB, RDS
- **Security:** AWS IAM
- **External APIs:** Weather and agricultural data sources

---

# Phase 3: Core Development

## Key Implementations
- Progressive Web App frontend
- Serverless backend services
- Image processing pipeline
- Real-time data ingestion
- Cloud database integration

## Development Approach
- Agile methodology
- Two-week sprints
- Continuous integration using GitHub
- Issue tracking via project board

---

# Phase 4: User Testing

## Testing Activities
- Task-based usability testing
- Feedback collection
- Performance evaluation
- Security validation

## Outcomes
- Identified UX improvements
- Bug and performance issue list
- Feature refinement priorities

---

# Phase 5: Refinement

- Bug fixes
- UI/UX improvements
- Performance optimisation
- Security hardening

---

# Phase 6: Final Testing & Deployment

## Testing
- System integration testing
- Regression testing
- Load and stress testing

## Deployment
- AWS cloud deployment
- Serverless configuration
- Scalability validation

## Documentation
- User guide
- System setup guide
- Troubleshooting manual

---

# 🧰 Technology Stack

| Layer | Technology |
|------|-----------|
| Frontend | React, PWA |
| Backend | AWS Lambda |
| APIs | API Gateway |
| Storage | S3, DynamoDB, RDS |
| Security | AWS IAM |
| DevOps | GitHub, CI/CD |
| AI | Image classification (ML) |

---

# 📂 Repository Structure
/frontend
/backend
/infrastructure
/docs
/tests



---

# 📅 Project Timeline

| Phase | Weeks |
|-----|------|
| Marketing Research & Prototyping | 1–2 |
| Requirements & Design | 3–4 |
| Core Development | 5–8 |
| User Testing | 9 |
| Refinement | 10–11 |
| Final Testing & Delivery | 12–13 |

---

# 👥 Team & Collaboration

- Scrum-based workflow
- Bi-weekly client updates
- Version control with GitHub
- Cloud-based deployment

---

| Name               | Role                                         | GitHub Profile                                                                                     | LinkedIn Profile                                                                                                              |
|--------------------|----------------------------------------------|----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| Nicholas Dobson    | Full-Stack Developer, DevOps                  | [<img src="assets/github-icon.png" alt="GitHub" width="40"/>](https://github.com/NicholasDobson)   | [<img src="assets/linkedin-icon-2.svg" alt="LinkedIn" width="40"/>](https://www.linkedin.com/in/nicholas-dobson-a64a84355)   |
| Mpho Tsotetsi      | Backend Developer                             | [<img src="assets/github-icon.png" alt="GitHub" width="40"/>](https://github.com/u22668323)        | [<img src="assets/linkedin-icon-2.svg" alt="LinkedIn" width="40"/>](https://www.linkedin.com/in/mpho-tsotetsi-256375287/)    |
| Taylor     | Full-Stack Developer, Team Lead               | [<img src="assets/github-icon.png" alt="GitHub" width="40"/>](https://github.com/ShaylinGovender) | [<img src="assets/linkedin-icon-2.svg" alt="LinkedIn" width="40"/>](https://www.linkedin.com/in/shaylin-govender-827347343)  |
| Lonwabo Kwitshana  | Frontend Developer, UI/UX                     | [<img src="assets/github-icon.png" alt="GitHub" width="40"/>](https://github.com/7onwabo)          | [<img src="assets/linkedin-icon-2.svg" alt="LinkedIn" width="40"/>](https://www.linkedin.com/in/lonwabo-kwitshana-b483831a6) |
| Diya     | Backend Developer, System Integration         | [<img src="assets/github-icon.png" alt="GitHub" width="40"/>](https://github.com/AryanMohanlall)   | [<img src="assets/linkedin-icon-2.svg" alt="LinkedIn" width="40"/>](https://www.linkedin.com/in/aryan-mohanlall-a45a89355)   |
<p align="right">
   <a href="#table-of-contents">Back to top</a>
</p>

---
# 📄 License

This project is developed for academic purposes as part of IMY 772 at the University of Pretoria.
