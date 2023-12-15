from arx import Dataset
from arx.aggregates import sum
from arx.criteria import KAnonymity

dataset_path = "./out/synthetic_data.csv"
anonymized_dataset_path = "./out/anonymized_dataset.csv"

df = pd.read_csv(dataset_path).infer_objects()
dataset = Dataset.from_pandas(df)

# Define quasi-identifiers
quasi_identifiers = ["age_month", "postcode", "body_length"]

# Set k for k-anonymity
k = 5

# Specify the privacy model (k-anonymity)
privacy_model = KAnonymity(5)

# Create a risk model based on the quasi-identifiers
risk_model = sum([dataset[qi] for qi in quasi_identifiers])

# Anonymize the dataset
anonymizer = risk_model.to_dataset_anonymizer(privacy_model=privacy_model)
anonymized_dataset = anonymizer.apply(dataset)

# Export the anonymized dataset
anonymized_df = anonymized_dataset.to_pandas()
anonymized_df.to_csv(anonymized_dataset_path, index=False)
