import pandas as pd
import os

dataset_path = "./out/synthetic_data.csv"

print("** Pandas ** ")
df = pd.read_csv(dataset_path).infer_objects()
print(df.dtypes)
