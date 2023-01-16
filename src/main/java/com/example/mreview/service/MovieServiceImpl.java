package com.example.mreview.service;

import com.example.mreview.dto.MovieDTO;
import com.example.mreview.dto.PageRequestDTO;
import com.example.mreview.dto.PageResultDTO;
import com.example.mreview.entity.Movie;
import com.example.mreview.entity.MovieImage;
import com.example.mreview.repository.MovieImageRepository;
import com.example.mreview.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Log4j2
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService{


    private final MovieRepository movieRepository;

    private final MovieImageRepository imageRepository;

    @Transactional
    @Override
    public Long register(MovieDTO movieDTO) {

        Map<String, Object> entityMap = dtoToEntity(movieDTO);
        Movie movie = (Movie) entityMap.get("movie");
        List<MovieImage> movieImageList = (List<MovieImage>) entityMap.get("imgList");

        movieRepository.save(movie);
        System.out.println(movieImageList);
        movieImageList.forEach(movieImage -> {
            imageRepository.save(movieImage);
        });

        return movie.getMno();
    }


    @Override
    public PageResultDTO<MovieDTO, Object[]> getList(PageRequestDTO requestDTO) {
        Pageable pageable = requestDTO.getPageable(Sort.by("mno").descending());

        Page<Object[]> result = movieRepository.getListPage(pageable);
        Function<Object[], MovieDTO> fn = (arr -> entitiestoDTO(
                (Movie) arr[0],
                (List<MovieImage>) (Arrays.asList((MovieImage)arr[1])),
                (Double)arr[2],
                (Long)arr[3])
        );

        return new PageResultDTO<>(result, fn);
    }

    @Override
    public MovieDTO getMovie(Long mno) {
        List<Object[]> result = movieRepository.getMovieWithAll(mno);

        Movie movie = (Movie) result.get(0)[0]; // Movie 엔티티는 가장 앞에 존재 - 모든 Row가 동일한 값

        List<MovieImage> movieImageList = new ArrayList<>();//영화의 이미지 개수만큼 MovieImage객체 필요

        result.forEach(arr -> {
            MovieImage movieImage = (MovieImage)arr[1];
            movieImageList.add(movieImage);
        });

        Double avg = (Double) result.get(0)[2]; //평균 평점 - 모든 Row가 동일한 값
        Long reviewCnt = (Long) result.get(0)[3]; //리뷰 개수 - 모든 Row가 동일한 값

        return entitiestoDTO(movie, movieImageList, avg, reviewCnt);

    }
}
