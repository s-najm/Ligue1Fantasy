## Ligue 1 Predictor using scikit-learn to predict from the matches.csv stat sheet containing data from all matches

import pandas as pd 
import numpy as np

# First, prepare the data from the main CSV file
print("🔄 Preparing Ligue 1 data...")
df = pd.read_csv("../football_matches_2024_2025.csv")

# Filter for Ligue 1 matches (FL1)
ligue1_df = df[df['competition_code'] == 'FL1'].copy()

# Select and rename columns to match the expected format
ligue1_df = ligue1_df[[
    'date_utc', 'home_team', 'away_team', 'fulltime_home', 'fulltime_away', 'status'
]].copy()

# Filter only finished matches
ligue1_df = ligue1_df[ligue1_df['status'] == 'FINISHED']

# Create home team records
home_df = ligue1_df.copy()
home_df['team'] = home_df['home_team']
home_df['opponent'] = home_df['away_team']
home_df['venue'] = 'Home'
home_df['gf'] = home_df['fulltime_home']
home_df['ga'] = home_df['fulltime_away']
home_df['result'] = np.where(home_df['gf'] > home_df['ga'], 'W',
                             np.where(home_df['gf'] == home_df['ga'], 'D', 'L'))

# Create away team records
away_df = ligue1_df.copy()
away_df['team'] = away_df['away_team']
away_df['opponent'] = away_df['home_team']
away_df['venue'] = 'Away'
away_df['gf'] = away_df['fulltime_away']
away_df['ga'] = away_df['fulltime_home']
away_df['result'] = np.where(away_df['gf'] > away_df['ga'], 'W',
                             np.where(away_df['gf'] == away_df['ga'], 'D', 'L'))

# Combine home and away records
matches = pd.concat([home_df, away_df], ignore_index=True)

# Add missing columns with placeholder values
matches['date'] = pd.to_datetime(matches['date_utc'])
matches['time'] = '20:00'  # Default time
matches['day'] = matches['date'].dt.strftime('%a')
matches['comp'] = 'Ligue 1'
matches['round'] = 'Matchweek ' + (matches.groupby('team').cumcount() + 1).astype(str)
matches['attendance'] = 0
matches['captain'] = ''
matches['formation'] = '4-3-3'
matches['referee'] = ''
matches['match report'] = ''
matches['notes'] = ''
matches['sh'] = np.random.randint(10, 25, size=len(matches))  # Random shots
matches['sot'] = np.random.randint(3, 10, size=len(matches))  # Random shots on target
matches['dist'] = np.random.uniform(15, 25, size=len(matches))  # Random shot distance
matches['fk'] = np.random.randint(0, 5, size=len(matches))  # Random free kicks
matches['pk'] = np.random.randint(0, 2, size=len(matches))  # Random penalties
matches['pkatt'] = matches['pk']  # Penalty attempts
matches['season'] = '2025'

print(f"✅ Loaded {len(matches)} Ligue 1 matches")

##converting all objects to int or float to be processed by the machine learning software
matches["date"] = pd.to_datetime(matches["date"])
matches["h/a"] = matches["venue"].astype("category").cat.codes ## converting venue to a home (1) or away (0) number
matches["opp"] = matches["opponent"].astype("category").cat.codes ## converting opponents to a number
matches["hour"] = matches["time"].str.replace(":.+", "", regex=True).astype("int") ## converting hours to number in case a team plays better at a certain time
matches["day"] = matches["date"].dt.dayofweek ## converting day of week of game to a number

matches["target"] = (matches["result"] == "W").astype("int") ## setting a win to the value 1

# Add team strength based on historical performance
team_stats = matches.groupby('team').agg({
    'gf': 'mean',
    'ga': 'mean',
    'target': 'mean'
}).round(2)
team_stats.columns = ['avg_gf', 'avg_ga', 'win_rate']

matches = matches.merge(team_stats, left_on='team', right_index=True)
matches = matches.merge(team_stats, left_on='opponent', right_index=True, suffixes=('', '_opp'))

from sklearn.ensemble import RandomForestClassifier ##importing machine learning for non linear data

rf = RandomForestClassifier(n_estimators = 200, min_samples_split=5, random_state=1, max_depth=10)
train = matches[matches["date"] < '2024-12-01'] 
test = matches[matches["date"] >= '2024-12-01']
predictors = ["h/a", "opp", "hour", "day", "avg_gf", "avg_ga", "win_rate", "avg_gf_opp", "avg_ga_opp", "win_rate_opp"]
rf.fit(train[predictors], train["target"])
RandomForestClassifier(min_samples_split = 10, n_estimators = 100, random_state = 1)
preds = rf.predict(test[predictors]) ##making prediction

from sklearn.metrics import accuracy_score
acc = accuracy_score(test["target"], preds) ## testing accuracy
acc
combined = pd.DataFrame(dict(actual=test["target"], prediction=preds))
pd.crosstab(index=combined["actual"], columns=combined["prediction"])

from sklearn.metrics import precision_score
precision_score(test["target"], preds)

grouped_matches = matches.groupby("team") 
group = grouped_matches.get_group("Paris Saint-Germain FC").sort_values("date")
 
def rolling_averages(group, cols, new_cols): ## function to take into consideration form of a team
    group = group.sort_values("date") ## sorting games by date 
    rolling_stats = group[cols].rolling(3, closed='left').mean()
    group[new_cols] = rolling_stats
    group = group.dropna(subset=new_cols) ##droping missing values and replacing with empty
    return group 

cols = ["gf", "ga", "sh", "sot", "dist", "fk", "pk", "pkatt"] 
new_cols = [f"{c}_rolling" for c in cols] ## creating new columns with rolling average values 

rolling_averages(group, cols, new_cols) ## calling function and generating average of last 3 games

matches_rolling = matches.groupby("team").apply(lambda x: rolling_averages(x, cols, new_cols), include_groups=False)
matches_rolling = matches_rolling.reset_index() ## reset index to get team column back

matches_rolling.index = range(matches_rolling.shape[0]) ## adding new index
matches_rolling
def make_predictions(data, predictors): ## making the predictions
    train = data[data["date"] < '2024-12-01'] 
    test = data[data["date"] >= '2024-12-01']
    rf.fit(train[predictors], train["target"])
    preds = rf.predict(test[predictors]) ##making prediction
    combined = pd.DataFrame(dict(actual=test["target"], prediction=preds), index=test.index)
    precision = precision_score(test["target"], preds)
    return combined, precision ## returning the values for the prediction

combined, precision = make_predictions(matches_rolling, predictors + new_cols)

precision

combined 

combined = combined.merge(matches_rolling[["date", "team", "opponent", "result"]], left_index = True, right_index = True)
combined

class MissingDict(dict): ## creating a class that inherits from the dictionary class
    __missing__ = lambda self, key: key ## in case a team name is missing

map_values = {
    "Paris Saint-Germain FC": "Paris Saint-Germain",
    "Olympique de Marseille": "Marseille",
    "AS Monaco FC": "Monaco",
    "OGC Nice": "Nice",
    "Olympique Lyonnais": "Lyon",
    "RC Strasbourg Alsace": "Strasbourg",
    "Racing Club de Lens": "Lens",
    "Stade Brestois 29": "Brest",
    "AS Saint-Étienne": "Saint-Étienne",
    "Lille OSC": "Lille",
    "Stade Rennais FC 1901": "Rennes",
    "FC Nantes": "Nantes",
    "Montpellier HSC": "Montpellier",
    "Angers SCO": "Angers",
    "Stade de Reims": "Reims",
    "Le Havre AC": "Le Havre",
    "AJ Auxerre": "Auxerre",
    "Toulouse FC": "Toulouse"
}
mapping = MissingDict(**map_values)
mapping["Paris Saint-Germain"]

combined["new_team"] = combined["team"].map(mapping)
combined

merged = combined.merge(combined, left_on=["date", "new_team"], right_on=["date", "opponent"]) ## finding both the home and away team predictions and merging them 

print("\n" + "="*80)
print("RÉSULTATS DU MODÈLE DE PRÉDICTION - LIGUE 1")
print("="*80)
print(f"\n📊 Précision du modèle: {precision:.1%}")
print(f"📈 Nombre total de prédictions: {len(combined)}")
print(f"\n✅ Prédictions correctes: {(combined['actual'] == combined['prediction']).sum()}")
print(f"❌ Prédictions incorrectes: {(combined['actual'] != combined['prediction']).sum()}")

# Calculate team-specific accuracy
team_accuracy = combined.groupby('team').apply(
    lambda x: (x['actual'] == x['prediction']).mean()
).sort_values(ascending=False).head(5)
print(f"\n🏆 Meilleures précisions par équipe:")
for team, acc in team_accuracy.items():
    print(f"   {team}: {acc:.1%}")

print("\n" + "-"*80)
print("EXEMPLES DE PRÉDICTIONS (10 premiers matchs)")
print("-"*80)
print(combined.head(10).to_string())

print("\n" + "-"*80)
print("PRÉDICTIONS FUSIONNÉES DOMICILE/EXTÉRIEUR (5 premiers)")
print("-"*80)
print(merged.head(5).to_string())

print("\n" + "="*80)


## project inspired by dataquest tutorial 
