<h1 align="center">🔲 Blackbox</h1>
 <p align="right">"Data is a precious thing and will last longer than the systems themselves." – Tim Berners-Lee</p>

<br/> 

<p>
 <img align="right" height="220" src="./docs/img/black-boxes.jpg" title="Black Boxes" alt="Black Boxes">
Meet Blackbox - a data exchange platform that helps to facilitate responsible access to collected and archived datasets within your organization. It helps you to:
<ul>
<li>Reduce the margin of risk, increase control, trust and awareness among data providers and consumers;</li>
<li>Handle data, models, and algorithms more responsibly;</li>
<li>Bring data protection dilemmas from moral into the practical plain of specific rules and responsibilities per-project.</li>
</ul>
</p>

## The big picture

<p>
Companies are increasingly coming under fire for the way they use data. Governments have amended many laws and regulations in response to this. GDPR law protects everyone's privacy. Under this law, the invasion of privacy or personal information leakage is punishable with heavy fines.
</p>

<p>
Values ​​such as "public safety" and "privacy" often conflict. Dilemmas arise when more weight is attached to one concept than another. Therefore, every good data analyst should have virtues such as "respect for the sensitivity of personal data" and "prudence and selectivity in communicating and sharing that data". Education and training in such virtues are crucial. Yet, the environment and technical facilities play a vital role in the ability of data professionals to make responsible choices as well.
</p>


## Technology ![Run SBT tests](https://github.com/mkotsur/blackbox/actions/workflows/run-sbt-test.yml/badge.svg)

Based on: [sara-nl/data-exchange](https://github.com/sara-nl/data-exchange)

### Modules

* [Backend](./backend/): Scala 3, CE3
* [Frontend](./frontend/): NextJS


## Documentation for users

*Coming up*

## Documentation for developers


### Starting server localy

1. Install Docker
2. Install SBT, NodeJS, yarn
3. Run in terminal: `cd backend && sbt "restApi/run"`
4. In a new terminal tab: `cd frontend && yarn install && yarn dev`
5. Open http://localhost:3000 🎉


## Want to know more?

Contact Mike Kotsur ([@mikekotsur](http://twitter.com/mikekotsur)) - [Absolute Value Labs](https://absolutevalue.nl)
