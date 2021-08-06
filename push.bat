set /p commit="Enter name of commit: "
git add .
git commit -m "%commit%"
git push heroku master
git push origin master