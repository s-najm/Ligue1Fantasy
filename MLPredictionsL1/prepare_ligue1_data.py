## Script to extract Ligue 1 data from football_matches_2024_2025.csv and prepare it for the predictor

import pandas as pd
import numpy as np
from datetime import datetime

# Read the main CSV file
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
matches_df = pd.concat([home_df, away_df], ignore_index=True)

# Add missing columns with placeholder values
matches_df['date'] = pd.to_datetime(matches_df['date_utc'])
matches_df['time'] = '20:00'  # Default time
matches_df['day'] = matches_df['date'].dt.strftime('%a')
matches_df['comp'] = 'Ligue 1'
matches_df['round'] = 'Matchweek ' + (matches_df.groupby('team').cumcount() + 1).astype(str)
matches_df['attendance'] = 0
matches_df['captain'] = ''
matches_df['formation'] = '4-3-3'
matches_df['referee'] = ''
matches_df['match report'] = ''
matches_df['notes'] = ''
matches_df['sh'] = np.random.randint(10, 25, size=len(matches_df))  # Random shots
matches_df['sot'] = np.random.randint(3, 10, size=len(matches_df))  # Random shots on target
matches_df['dist'] = np.random.uniform(15, 25, size=len(matches_df))  # Random shot distance
matches_df['fk'] = np.random.randint(0, 5, size=len(matches_df))  # Random free kicks
matches_df['pk'] = np.random.randint(0, 2, size=len(matches_df))  # Random penalties
matches_df['pkatt'] = matches_df['pk']  # Penalty attempts
matches_df['season'] = '2025'

# Select and order columns as expected by the predictor
columns_order = [
    'date', 'time', 'comp', 'round', 'day', 'venue', 'result', 'gf', 'ga',
    'opponent', 'attendance', 'captain', 'formation', 'referee', 'match report',
    'notes', 'sh', 'sot', 'dist', 'fk', 'pk', 'pkatt', 'season', 'team'
]

matches_df = matches_df[columns_order]

# Save to matches.csv
matches_df.to_csv('matches.csv', index=False)

print(f"✅ Successfully extracted {len(matches_df)} Ligue 1 matches")
print(f"📊 Teams included: {sorted(matches_df['team'].unique())}")
print(f"\n📅 Date range: {matches_df['date'].min()} to {matches_df['date'].max()}")
print(f"\n📈 Results distribution:")
print(matches_df['result'].value_counts())
