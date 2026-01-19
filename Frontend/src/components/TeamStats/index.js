import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useSearchParams } from 'react-router-dom';
import Loader from "react-loaders";
import AnimatedLetters from "../AnimatedLetters";
import "./index.scss";

const TeamStats = () => {
  const [searchParams] = useSearchParams();
  const teamName = searchParams.get('team');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [h2hData, setH2hData] = useState([]);
  const [letterClass, setLetterClass] = useState('text-animate');

  useEffect(() => {
    const timer = setTimeout(() => {
      setLetterClass("text-animate-hover");
    }, 3000);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (teamName) {
      axios.get(`http://localhost:9090/api/v1/ligue1/h2h?team=${encodeURIComponent(teamName)}`)
        .then(response => {
          setH2hData(response.data);
          setLoading(false);
        })
        .catch(error => {
          setError(error);
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [teamName]);

  if (loading) {
    return (
      <>
        <div className="container team-stats-page">
          <h1 className="page-title">Loading...</h1>
        </div>
        <Loader type="pacman" />
      </>
    );
  }

  if (error) {
    return (
      <>
        <div className="container team-stats-page">
          <h1 className="page-title">Error loading team statistics</h1>
          <p className="error-message">{error.message}</p>
        </div>
        <Loader type="pacman" />
      </>
    );
  }

  const calculateTotals = () => {
    if (!h2hData || h2hData.length === 0) return null;
    
    return h2hData.reduce((acc, row) => ({
      mp: acc.mp + row.mp,
      w: acc.w + row.w,
      d: acc.d + row.d,
      l: acc.l + row.l,
      gf: acc.gf + row.gf,
      ga: acc.ga + row.ga,
      gd: acc.gd + row.gd,
      pts: acc.pts + row.pts
    }), { mp: 0, w: 0, d: 0, l: 0, gf: 0, ga: 0, gd: 0, pts: 0 });
  };

  const totals = calculateTotals();

  return (
    <>
      <div className="container team-stats-page">
        <h1 className="page-title">
          <AnimatedLetters 
            letterClass={letterClass} 
            strArray={`${teamName} Statistics`.split("")} 
            idx={15}
          />
        </h1>

        {totals && (
          <div className="team-summary">
            <h2>Season Overview</h2>
            <div className="summary-grid">
              <div className="stat-card">
                <div className="stat-value">{totals.mp}</div>
                <div className="stat-label">Matches Played</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{totals.w}</div>
                <div className="stat-label">Wins</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{totals.d}</div>
                <div className="stat-label">Draws</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{totals.l}</div>
                <div className="stat-label">Losses</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{totals.gf}</div>
                <div className="stat-label">Goals For</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{totals.ga}</div>
                <div className="stat-label">Goals Against</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{totals.gd > 0 ? `+${totals.gd}` : totals.gd}</div>
                <div className="stat-label">Goal Difference</div>
              </div>
              <div className="stat-card highlight">
                <div className="stat-value">{totals.pts}</div>
                <div className="stat-label">Points</div>
              </div>
            </div>
          </div>
        )}

        <div className="h2h-section">
          <h2>Head-to-Head Records</h2>
          <div className="table-container">
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
                {h2hData.map((row, idx) => (
                  <tr key={idx}>
                    <td className="opponent-name">{row.opponent}</td>
                    <td>{row.mp}</td>
                    <td>{row.w}</td>
                    <td>{row.d}</td>
                    <td>{row.l}</td>
                    <td>{row.gf}</td>
                    <td>{row.ga}</td>
                    <td className={row.gd > 0 ? 'positive' : row.gd < 0 ? 'negative' : ''}>
                      {row.gd > 0 ? `+${row.gd}` : row.gd}
                    </td>
                    <td className="points">{row.pts}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <Loader type="pacman" />
    </>
  );
};

export default TeamStats;
