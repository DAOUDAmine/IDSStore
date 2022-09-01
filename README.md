# IDSStore Project
## Project Title:
A Meta-IDS Repository for Capitalizing and Reproducing Intrusion Detection Systems for Cyber Security Stakeholders
### Project Discription
IDSStore ensures the IDS results' reproducibility, this article proposes a framework that facilitates the reuse of existing IDS to solve a diversity of problems for the cyber security stakeholders (i.e. end-users and experts). Firstly, a descriptive language is proposed to explicitly specify the IDS descriptions. Secondly, a model repository allows the reusability and sharing of the IDS configurations. Finally, to demonstrate the feasibility of our framework, we evaluate it against a case study to emphasize the repository and its significance to address the challenges related to data acquisition and knowledge organization/sharing Our findings show that our framework provides satisfactory prediction accuracy for the configurations' reuse and identifies the right reusable components with precision.

![image](https://user-images.githubusercontent.com/42803883/187884037-194f9867-095b-401b-9f92-e4562b98254d.png)

### Implementation
IDSStore has been implemented as as open-source Eclipse plugins. Our solution offer possible way to create a IDS configuration expressed in IDSDL design language: via a tool based on Java EMF (Eclipse Modeling Framework) API and has been integrated as a plugin in Eclipse (Eclipse Modeling Project. www.eclipse.org/modeling/) which is an integrated development environment (IDE). Through the editor tool, every IDS instance is saved as an XMI (XML Metadata Interchange) file. 
