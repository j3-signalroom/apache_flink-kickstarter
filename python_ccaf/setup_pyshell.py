from pyflink.table.confluent import *
from pyflink.table import *
from pyflink.table.expressions import *

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


settings = ConfluentSettings.from_global_variables()
tbl_env = TableEnvironment.create(settings)

print()
print()
print("Welcome to Confluent Cloud Apache Flink® (CCAF) Table API")
print()
print()
print("A TableEnvironment has been pre-initialized and is available under `tbl_env`.")
print()
print("Some inspirations to get started from examples courtesy of your CCAF:")
print("  - Say hello: tbl_env.execute_sql(\"SELECT 'Hello world!'\").print()")
print("  - List catalogs: tbl_env.list_catalogs()")
print("  - Show something fancy: tbl_env.from_path(\"examples.marketplace.clicks\").execute().print()")
print()
print()
