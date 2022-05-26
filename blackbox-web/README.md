This is frontend for the [blackbox](https://github.com/mkotsur/blackbox)

## Demo data

R script:

```r
df = data.frame(matrix(rnorm(20), nrow=10))
df
```

R script with accessing a "sensitive" dataset:

```
data <- read.csv(file = '/tmp/data/world_population.csv')

print('Max population')
data[which.max(data$Population..2020),]

print('Min population')
data[which.min(data$Population..2020),]
```

R Markdown script:

````
---
title: "Markdown demo"
output: html_document
---
# This is merely a demo of R Markdown

```{r}
data <- read.csv(file = '/tmp/data/world_population.csv')
hist(data$Population, breaks=100)
```
````

Python script:

```python
import calendar
from datetime import datetime

now = datetime.now()
print(calendar.month(now.year, now.month))
```

## Development

First, run the development server:

```bash
yarn dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.
