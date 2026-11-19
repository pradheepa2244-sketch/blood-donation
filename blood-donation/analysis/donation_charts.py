from getpass import getpass
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator
import pandas as pd


def read_frame(conn, sql):
    with conn.cursor() as cursor:
        cursor.execute(sql)
        return pd.DataFrame(cursor.fetchall(), columns=[col.name for col in cursor.description])


def chart(values, title, ylabel, path):
    ax = values.plot.bar(color="#a6294b", figsize=(8, 4), rot=20)
    ax.set(title=title, xlabel="", ylabel=ylabel)
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))
    ax.figure.tight_layout()
    ax.figure.savefig(path, dpi=160)
    plt.close(ax.figure)


def generate(donations, stock, output):
    donations = donations.copy()
    stock = stock.copy()
    donations["donated_on"] = pd.to_datetime(donations["donated_on"], errors="coerce")
    donations["units"] = pd.to_numeric(donations["units"], errors="coerce")
    donations["blood_group"] = donations["blood_group"].astype("string").str.strip()
    donations = donations.dropna().drop_duplicates("id")
    donations = donations[donations["units"] > 0].copy()
    stock["units"] = pd.to_numeric(stock["units"], errors="coerce")
    stock = stock.dropna().drop_duplicates("blood_group").set_index("blood_group")["units"].sort_index()
    if donations.empty or stock.empty:
        print("Load the demo data before generating the three charts.")
        return
    output.mkdir(parents=True, exist_ok=True)
    by_group = donations.groupby("blood_group").size().reindex(stock.index, fill_value=0)
    by_month = donations.groupby(donations["donated_on"].dt.to_period("M"))["units"].sum().sort_index()
    by_month = by_month.reindex(pd.period_range(by_month.index.min(), by_month.index.max(), freq="M"), fill_value=0)
    by_month.index = by_month.index.astype(str)
    chart(by_group, "Donations by Blood Group", "Donation records", output / "donations_by_group.png")
    chart(by_month, "Monthly Donation Units", "Units donated", output / "monthly_donation_units.png")
    chart(stock, "Current Stock by Blood Group", "Units in stock", output / "stock_by_group.png")
    print(f"Saved three charts from {len(donations)} donation records.")


def main():
    import psycopg
    password = getpass("PostgreSQL password for postgres: ")
    with psycopg.connect(host="localhost", port=5432, dbname="blood_donation",
                         user="postgres", password=password) as conn:
        donations = read_frame(conn, "SELECT id,blood_group,donated_on,units FROM donations ORDER BY id")
        stock = read_frame(conn, "SELECT blood_group,units FROM stock ORDER BY blood_group")
    generate(donations, stock, Path(__file__).resolve().parent / "output")


if __name__ == "__main__":
    main()
