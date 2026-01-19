@echo off
echo ========================================
echo    LIGUE 1 MATCH PREDICTOR
echo ========================================
echo.

echo Installing required packages...
pip install pandas scikit-learn numpy

echo.
echo Running the predictor...
python PL_Predictor.py

echo.
echo ========================================
echo    Prediction complete!
echo ========================================
pause
