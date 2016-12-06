var gulp = require('gulp'),
    autoprefixer = require('gulp-autoprefixer'),
    bower = require('gulp-bower'),
    concat = require('gulp-concat'),
    filter = require('gulp-filter'),
    flatten = require('gulp-flatten'),
    mainBowerFiles = require('main-bower-files'),
    minifycss = require('gulp-minify-css'),
    order = require('gulp-order'),
    plumber = require('gulp-plumber'),
    sass = require('gulp-sass'),
    sequence = require('gulp-sequence'),
    uglify = require('gulp-uglify'),
    watch = require('gulp-watch'),
    webserver = require('gulp-webserver');

gulp.task('bower-install', function () {
  return bower();
});

gulp.task('vendor-fonts', function () {
  const fontFilter = filter(['**/*.eot', '**/*.woff*', '**/*.svg', '**/*.ttf']);

  gulp.src(mainBowerFiles())
      .pipe(fontFilter)
      .pipe(flatten())
      .pipe(gulp.dest('build/fonts'))
});

gulp.task('vendor-scripts', function () {
  const jsFilter = filter(['**/*.js']);

  gulp.src(mainBowerFiles())
      .pipe(jsFilter)
      .pipe(plumber())
      .pipe(flatten())
      .pipe(order([
        'jquery.js',
        '**'
      ]))
      .pipe(concat('vendor.js'))
      .pipe(gulp.dest('build/js'))
});

gulp.task('vendor', sequence('bower-install', ['vendor-fonts', 'vendor-scripts']));

gulp.task('scripts', function () {
  gulp.src('js/**/*.js')
      .pipe(plumber())
      .pipe(flatten())
      .pipe(concat('app.js'))
      .pipe(gulp.dest('build/js'));
});

gulp.task('sass', function(){
  gulp.src('sass/**/*.scss')
      .pipe(plumber())
      .pipe(sass({
        includePaths: [
          'sass',
          'bower_components/normalize-scss/sass/',
          'bower_components/bourbon/app/assets/stylesheets/',
          'bower_components/bitters/app/assets/stylesheets',
          'bower_components/neat/app/assets/stylesheets/',
          'bower_components/fontawesome/scss/'
        ]
        //style: 'compressed'
      }).on('error', sass.logError))
      .pipe(autoprefixer(
        'last 2 version',
        '> 1%',
        'ie 8',
        'ie 9',
        'ios 6',
        'android 4'
      ))
      //.pipe(minifycss())
      .pipe(gulp.dest('build/css'));
});

gulp.task('webserver', function () {
  gulp.src('./')
      .pipe(webserver({
          host: '0.0.0.0',
          port: 8080,
          path: '/monglorious',
          livereload: true,
          open: 'http://localhost:8080/monglorious'
      }));
});

gulp.task('watch', function () {
  watch('js/**/*.js', function () { gulp.start(['scripts']); });
  watch('sass/**/*.scss', function () { gulp.start(['sass']); });
});

gulp.task('build', sequence('vendor', ['scripts', 'sass']));
gulp.task('server', sequence('build', ['watch', 'webserver']));

gulp.task('default', ['build']);
