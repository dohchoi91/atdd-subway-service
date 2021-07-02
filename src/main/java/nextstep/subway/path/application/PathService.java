package nextstep.subway.path.application;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Objects;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedMultigraph;
import org.springframework.stereotype.Service;

import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.path.exception.DuplicatePathException;
import nextstep.subway.path.exception.NotConnectedPathException;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import nextstep.subway.station.dto.StationResponse;
import nextstep.subway.station.exeption.NotFoundStationException;

@Service
public class PathService {

    private StationRepository stationRepository;
    private LineRepository lineRepository;

    public PathService(StationRepository stationRepository, LineRepository lineRepository) {
        this.stationRepository = stationRepository;
        this.lineRepository = lineRepository;
    }

    public PathResponse findPath(Long source, Long target) {
        Station start = stationRepository.findById(source).orElseThrow(NotFoundStationException::new);
        Station end = stationRepository.findById(target).orElseThrow(NotFoundStationException::new);

        if (start.equals(end)) {
            throw new DuplicatePathException();
        }

        WeightedMultigraph<Station, DefaultWeightedEdge> map = getPathMap();
        GraphPath<Station, DefaultWeightedEdge> path = findShortestPath(map, start, end);

        List<StationResponse> collect = path.getVertexList().stream().map(StationResponse::of).collect(toList());
        return PathResponse.of(collect, path.getWeight());
    }

    private GraphPath<Station, DefaultWeightedEdge> findShortestPath(WeightedMultigraph<Station, DefaultWeightedEdge> map,
                                                                     Station start, Station end) {
        GraphPath<Station, DefaultWeightedEdge> path = new DijkstraShortestPath(map).getPath(start, end);
        if (Objects.isNull(path)) {
            throw new NotConnectedPathException();
        }
        return path;
    }

    private WeightedMultigraph<Station, DefaultWeightedEdge> getPathMap() {
        WeightedMultigraph<Station, DefaultWeightedEdge> map = new WeightedMultigraph(DefaultWeightedEdge.class);
        lineRepository.findAll().forEach(line -> {
            line.getStations().forEach(map::addVertex);
            line.getSections().forEach(section -> {
                DefaultWeightedEdge edge = map.addEdge(section.getUpStation(), section.getDownStation());
                map.setEdgeWeight(edge, section.getDistance());
            });
        });
        return map;
    }
}
