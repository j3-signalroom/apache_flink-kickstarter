from pyflink.table import TableEnvironment
from pyflink.table.confluent import ConfluentSettings
from pyflink.table.expressions import *

from src.kickstarter.helper.settings import CC_PROPERTIES_PATHNAME


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


settings = ConfluentSettings.from_file(CC_PROPERTIES_PATHNAME)
tbl_env = TableEnvironment.create(settings)

print()
print()
print("Welcome to Confluent Cloud Apache Flink® (CCAF) Table API")
print()
print()
print("A TableEnvironment has been pre-initialized and is available under `tbl_env`.")
print()
print("Some example calls:")
print("  - List catalogs: tbl_env.list_catalogs()")
print("  - Get a catalog object: catalog = tbl_env.get_catalog('<catalog_name>')")
print("  - Get a list of a catalog's databases: catalog.list_databases()")
print()
