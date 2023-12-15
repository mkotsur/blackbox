from faker import Faker
import pandas as pd
import os

fake = Faker("nl_NL")

num_rows = 100  # Change this to the desired number of rows

column_descriptions = {
    "name": "Full Name",
    "email": "Email Address",
    "age_month": "Age in months",
    "postcode": "Post code",
    "body_length": "Body length in cm",
    "body_weight": "Body weight in g",
}

out_path = "./out"

age_min = 0
age_max = 120

body_lenght_min = 46
body_lenght_max = 146

body_weigth_min = 3400
body_weigth_max = 46000


synthetic_data = {column_name: [] for column_name in column_descriptions.keys()}

for _ in range(num_rows):
    synthetic_data["name"].append(fake.name())
    synthetic_data["email"].append(fake.email())
    synthetic_data["age_month"].append(fake.random_int(min=age_min, max=age_max))
    synthetic_data["postcode"].append(fake.postcode())
    synthetic_data["body_length"].append(
        fake.random_int(min=body_lenght_min, max=body_lenght_max)
    )
    synthetic_data["body_weight"].append(
        fake.random_int(min=body_weigth_min, max=body_weigth_max)
    )

df = pd.DataFrame(synthetic_data)

if not os.path.exists(out_path):
    os.mkdir(out_path)

df.to_csv(os.path.join(out_path, "synthetic_data.csv"), index=False)
