import React, { useState, useEffect } from 'react';
import axios from 'axios';
import "./index.scss";
import AnimatedLetters from "../AnimatedLetters";

const TeamData = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [playerData, setPlayerData] = useState([]);
  const [standings, setStandings] = useState(null);
  const [h2hData, setH2hData] = useState([]);
  const [playersToShow, setPlayersToShow] = useState(10);
  const [letterClass] = useState('text-animate');
  const [pageTitle, setPageTitle] = useState('Player Data');
  const [teamValue, setTeamValue] = useState(null);
  
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const teamValueFromQuery = params.get('team');
    const nationValue = params.get('nation');
    const positionValue = params.get('position');
    const nameValue = params.get('name');
    
    if (teamValueFromQuery) {
      setTeamValue(teamValueFromQuery);
      setPageTitle(teamValueFromQuery.split("-").join(" "));

      Promise.allSettled([
        axios.get(`http://localhost:9090/api/v1/ligue1/standings`),
        axios.get(`http://localhost:9090/api/v1/ligue1/h2h?team=${encodeURIComponent(teamValueFromQuery)}`),
        axios.get(`http://localhost:9090/api/v1/player?team=${encodeURIComponent(teamValueFromQuery)}`)
      ]).then(([standingsResult, h2hResult, playersResult]) => {
        if (standingsResult.status === 'fulfilled') {
          setStandings(standingsResult.value.data);
        }
        if (h2hResult.status === 'fulfilled') {
          setH2hData(h2hResult.value.data);
        }
        if (playersResult.status === 'fulfilled') {
          setPlayerData(playersResult.value.data);
        } else if (playersResult.status === 'rejected') {
          setError(playersResult.reason);
        }
        setLoading(false);
      });
    } else if (nationValue){
      setPageTitle('Player Data');
      axios.get(`http://localhost:9090/api/v1/player?nation=${encodeURIComponent(nationValue)}`)
      .then(response => {
        setPlayerData(response.data);
        setLoading(false);
      })
      .catch(error => {
        setError(error);
        setLoading(false);
      });
    } else if (positionValue){
      setPageTitle('Player Data');
      axios.get(`http://localhost:9090/api/v1/player?position=${encodeURIComponent(positionValue)}`)
      .then(response => {
        setPlayerData(response.data);
        setLoading(false);
      })
      .catch(error => {
        setError(error);
        setLoading(false);
      });
    } else if (nameValue){
      setPageTitle('Player Data');
      axios.get(`http://localhost:9090/api/v1/player?name=${encodeURIComponent(nameValue)}`)
      .then(response => {
        setPlayerData(response.data);
        setLoading(false);
      })
      .catch(error => {
        setError(error);
        setLoading(false);
      });
    }
      else {
      setLoading(false);
    }
  }, []);

  if (loading) {
    return <p>Loading...</p>;
  }

  if (error) {
    return <p>Error: {error.message}</p>;
  }

  const normalize = (value) => {
    if (!value) return '';
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/&/g, 'and')
      .replace(/[^a-z0-9]+/g, '');
  };

  const renderLeagueTable = (table, highlightedTeam) => {
    if (!table || !table.columns || !table.rows) return null;
    const highlighted = normalize(highlightedTeam);
    return (
      <table>
        <thead>
          <tr>
            {table.columns.map((c) => (
              <th key={c}>{c}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {table.rows.map((r, idx) => (
            <tr
              key={idx}
              style={
                r && r.values && r.values.Squad && highlighted
                  ? {
                      backgroundColor:
                        normalize(r.values.Squad) === highlighted ||
                        normalize(r.values.Squad).includes(highlighted) ||
                        highlighted.includes(normalize(r.values.Squad))
                          ? '#3a3a5a'
                          : undefined
                    }
                  : undefined
              }
            >
              {table.columns.map((c) => (
                <td key={c}>{r.values && r.values[c] ? r.values[c] : ""}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    );
  };

  const renderH2H = (rows) => {
    if (!rows || rows.length === 0) return null;
    return (
      <table>
        <thead>
          <tr>
            <th>Opponent</th>
            <th>MP</th>
            <th>W</th>
            <th>D</th>
            <th>L</th>
            <th>GF</th>
            <th>GA</th>
            <th>GD</th>
            <th>Pts</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={idx}>
              <td>{row.opponent}</td>
              <td>{row.mp}</td>
              <td>{row.w}</td>
              <td>{row.d}</td>
              <td>{row.l}</td>
              <td>{row.gf}</td>
              <td>{row.ga}</td>
              <td>{row.gd}</td>
              <td>{row.pts}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  };

  return (
    <div className={`fade-in ${loading ? 'loading' : ''}`}>
    <div className="table-container">
      <h1 className = "page-title">
        <AnimatedLetters letterClass = {letterClass} strArray={pageTitle.split("")} idx={12}/>
      </h1>
      {teamValue && standings && (
        <>
          <h2 style={{ color: '#d4af37' }}>Overall</h2>
          {renderLeagueTable(standings.overall, teamValue)}
          <br/>
          <h2 style={{ color: '#d4af37' }}>Home/Away</h2>
          {renderLeagueTable(standings.homeAway, teamValue)}
          <br/>
          <h2 style={{ color: '#d4af37' }}>Head-to-Head</h2>
          {renderH2H(h2hData)}
          <br/>
          <h2 style={{ color: '#d4af37' }}>Players</h2>
        </>
      )}
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Position</th>
            <th>Age</th>
            <th>Matches Played</th>
            <th>Starts</th>
            <th>Minutes Played</th>
            <th>Goals</th>
            <th>Assists</th>
            <th>Penalties Kicked</th>
            <th>Yellow Cards</th>
            <th>Red Cards</th>
            <th>Expected Goals (xG)</th>
            <th>Expected Assists (xAG)</th>
            <th>Team</th>
          </tr>
        </thead>
        <tbody>
          {playerData.slice(0, playersToShow).map(player => (
            <tr key={player.name}>
              <td>{player.name}</td>
              <td>{player.pos}</td>
              <td>{player.age}</td>
              <td>{player.mp}</td>
              <td>{player.starts}</td>
              <td>{player.min}</td>
              <td>{player.gls}</td>
              <td>{player.ast}</td>
              <td>{player.pk}</td>
              <td>{player.crdy}</td>
              <td>{player.crdr}</td>
              <td>{player.xg}</td>
              <td>{player.xag}</td>
              <td>{player.team}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {playersToShow < playerData.length && (
        <button onClick={() => setPlayersToShow(playersToShow + 10)} style={{ marginTop: '10px', marginBottom: '10px' }} className={`show-more-button ${loading ? 'loading' : ''}`}>
          Show More
        </button>
      )}
    </div>
    </div>
  );
};

export default TeamData;
